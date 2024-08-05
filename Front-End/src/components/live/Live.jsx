import { OpenVidu } from "openvidu-browser";
import axios from "axios";
import React, { useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import styles from "./Live.module.css";
import UserVideoComponent from "./UserVideoComponent";
import truckImg from "assets/images/storeImg.png";
import Modal from "./Modal";
import OpenClose from "components/owner/mainPage/OpenClose";
import JiguemOrder from "components/owner/mainPage/JiguemOrder";

const APPLICATION_SERVER_URL = "https://i11b102.p.ssafy.io/";
// const APPLICATION_SERVER_URL = "http://localhost:5000/";

const Live = () => {
  const role = sessionStorage.getItem("role");
  const { storeId } = useParams();
  const [mySessionId, setMySessionId] = useState(storeId); //세션 아이디를 스토어아이디로 설정 -> 나중에 보안을 위해 수정해야될지도
  const [myUserName, setMyUserName] = useState(
    sessionStorage.getItem("nickname")
  );
  const [session, setSession] = useState(undefined);
  const [mainStreamManager, setMainStreamManager] = useState(undefined);
  const [publisher, setPublisher] = useState(undefined);
  const [subscribers, setSubscribers] = useState([]);
  const [isChat, setIsChat] = useState(false);
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState("");
  const [storeInfo, setStoreInfo] = useState({
    ceo: "푸바오",
    truck: "울퉁불퉁",
  });
  const [storeNotice, setStoreNotice] = useState(
    "월 수 금 15:00~22:00 운영합니다 \n비오면 안나가요 \n07.19(금) 팥붕 안팔아요"
  );
  const OV = useRef();
  const [modalMessage, setModalMessage] = useState("");

  useEffect(() => {
    // // 초기 더미 데이터 설정 (사장님 역할)
    // sessionStorage.setItem("role", "owner");
    // sessionStorage.setItem("nickname", "푸바오");

    window.addEventListener("beforeunload", onbeforeunload);
    return () => {
      window.removeEventListener("beforeunload", onbeforeunload);
    };
  }, []);

  useEffect(() => {
    if (role === "owner") {
      createSessionAndJoin(true); // 퍼블리셔로 참여
    }
  }, []);

  const onbeforeunload = (event) => {
    leaveSession();
  };

  const handleChangeSessionId = (e) => {
    setMySessionId(e.target.value);
  };

  const handleChangeUserName = (e) => {
    setMyUserName(e.target.value);
  };

  const handleMainVideoStream = (stream) => {
    if (mainStreamManager !== stream) {
      setMainStreamManager(stream);
    }
  };

  const deleteSubscriber = (streamManager) => {
    const newSubscribers = subscribers.filter((sub) => sub !== streamManager);
    setSubscribers(newSubscribers);
  };

  const joinSession = async (e) => {
    e.preventDefault();
    const role = sessionStorage.getItem("role");

    if (role === "owner") {
      await createSessionAndJoin(true); // 퍼블리셔로 참여
    } else if (role === "customer") {
      try {
        await createSessionAndJoin(false); // 구독자로 참여
      } catch (error) {
        setModalMessage("현재 방송 중이 아닙니다!");
      }
    }
  };

  const createSessionAndJoin = async (isPublisher) => {
    OV.current = new OpenVidu();
    const newSession = OV.current.initSession();

    setSession(newSession);

    newSession.on("streamCreated", (event) => {
      const subscriber = newSession.subscribe(event.stream, undefined);
      setSubscribers((prevSubscribers) => [...prevSubscribers, subscriber]);

      // 퍼블리셔 스트림이 생성될 때 메인 스트림 매니저로 설정
      if (!isPublisher) {
        setMainStreamManager(subscriber);
      }
    });

    newSession.on("streamDestroyed", (event) => {
      deleteSubscriber(event.stream.streamManager);
    });

    newSession.on("exception", (exception) => {
      console.warn(exception);
    });

    newSession.on("signal:my-chat", (event) => {
      const message = event.data.split(",");
      const from = message[0];
      const msg = message[1];
      setMessages((prevMessages) => [...prevMessages, { from, message: msg }]);
    });

    try {
      const token = await getToken();
      await newSession.connect(token, { clientData: myUserName });

      if (isPublisher) {
        let newPublisher = await OV.current.initPublisherAsync(undefined, {
          audioSource: undefined,
          videoSource: undefined,
          publishAudio: true,
          publishVideo: true,
          resolution: "640x480",
          frameRate: 30,
          insertMode: "APPEND",
          mirror: true,
        });

        newSession.publish(newPublisher);
        setPublisher(newPublisher); // 퍼블리셔 설정
        setMainStreamManager(newPublisher);
      }
    } catch (error) {
      if (!isPublisher) {
        throw new Error("Session not found");
      }
      console.log(
        "There was an error connecting to the session:",
        error.code,
        error.message
      );
    }
  };

  const leaveSession = () => {
    if (session) {
      session.disconnect();
    }

    OV.current = null;
    setSession(undefined);
    setSubscribers([]);
    setMySessionId("SessionA");
    setMyUserName(sessionStorage.getItem("nickname"));
    setMainStreamManager(undefined);
    setPublisher(undefined);
  };

  const switchCamera = async () => {
    try {
      const devices = await OV.current.getDevices();
      const videoDevices = devices.filter(
        (device) => device.kind === "videoinput"
      );

      if (videoDevices && videoDevices.length > 1) {
        const newVideoDevice = videoDevices.filter(
          (device) =>
            device.deviceId !==
            publisher.stream.getMediaStream().getVideoTracks()[0].getSettings()
              .deviceId
        );

        if (newVideoDevice.length > 0) {
          const newPublisher = OV.current.initPublisher(undefined, {
            videoSource: newVideoDevice[0].deviceId,
            publishAudio: true,
            publishVideo: true,
            mirror: true,
          });

          await session.unpublish(publisher);
          await session.publish(newPublisher);

          setPublisher(newPublisher);
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  const toggleChat = () => {
    setIsChat(!isChat);
  };

  const handleMessageChange = (e) => {
    setMessage(e.target.value);
  };

  const sendMessage = (e) => {
    e.preventDefault();
    if (message.trim() !== "") {
      const nickname = sessionStorage.getItem("nickname");
      session
        .signal({
          data: `${nickname},${message}`,
          to: [],
          type: "my-chat",
        })
        .then(() => {
          console.log(message);
          console.log("Message successfully sent");
          setMessage("");
        })
        .catch((error) => {
          console.error(error);
        });
    }
  };

  const getToken = async () => {
    const sessionId = await createSession(mySessionId);
    console.log("세션아이디" + sessionId);

    return await createToken(sessionId);
  };

  const createSession = async (sessionId) => {
    const response = await axios.post(
      APPLICATION_SERVER_URL + "api/sessions",
      { customSessionId: sessionId },
      {
        headers: { "Content-Type": "application/json" },
      }
    );

    return response.data; // The sessionId
  };

  const createToken = async (sessionId) => {
    const response = await axios.post(
      APPLICATION_SERVER_URL + "api/sessions/" + sessionId + "/connections",
      {},
      {
        headers: { "Content-Type": "application/json" },
      }
    );
    return response.data; // The token
  };

  const closeModal = () => {
    setModalMessage("");
  };

  //공지사항 작성 버튼
  const noticeRegistClick = () => {};

  return (
    <div className={styles.container}>
      {modalMessage && <Modal message={modalMessage} onClose={closeModal} />}

      {session !== undefined ? (
        <div className={styles.session}>
          <div className={styles.sessionHeader}>
            <h1 className={styles.sessionTitle}>{mySessionId}</h1>
            <input
              className={`${styles.btn} ${styles.btnLarge} ${styles.btnDanger}`}
              type="button"
              id="buttonLeaveSession"
              onClick={leaveSession}
              value="Leave session"
            />
            <input
              className={`${styles.btn} ${styles.btnLarge} ${styles.btnSuccess}`}
              type="button"
              id="buttonSwitchCamera"
              onClick={switchCamera}
              value="Switch Camera"
            />
            <button
              className={`${styles.btn} ${styles.btnLarge} ${styles.btnInfo}`}
              id="buttonChat"
              onClick={toggleChat}
            >
              {isChat ? "Close Chat" : "Open Chat"}
            </button>
            <button
              className={`${styles.btn} ${styles.btnLarge} ${styles.btnInfo}`}
              id="noticeRegist"
              onClick={noticeRegistClick}
            >
              공지사항 작성
            </button>
          </div>

          {mainStreamManager !== undefined ? (
            <div className={styles.mainVideo}>
              <UserVideoComponent streamManager={mainStreamManager} />
            </div>
          ) : null}

          {isChat ? (
            <div className={styles.chatContainer}>
              <div className={styles.chatInfo}>
                <p>
                  <span className={styles.infoGreen}>{storeInfo.ceo}</span>{" "}
                  사장님이 운영하는
                </p>
                <p>
                  <span className={styles.infoGreen}>{storeInfo.truck}</span>{" "}
                  트럭의 채팅방입니다
                </p>
              </div>
              <div className={styles.noticeBox}>
                <div>
                  <img
                    className={styles.truckImg}
                    src={truckImg}
                    alt="트럭이미지"
                  />
                </div>
                <div className={styles.noticeInfo}>
                  <div className={styles.noticeTitle}>📌 사장님 공지사항</div>
                  <div className={styles.noticeContent}>{storeNotice}</div>
                </div>
              </div>
              <div className={styles.chatBox}>
                <div className={styles.messageList}>
                  {messages.map((msg, i) => (
                    <div
                      key={i}
                      className={`${styles.message} ${
                        msg.from === storeInfo.ceo
                          ? styles.messageOwner
                          : styles.messageCustomer
                      }`}
                    >
                      <div>
                        <b
                          className={`${
                            msg.from === storeInfo.ceo
                              ? styles.messageFromOwner
                              : styles.messageFromCustomer
                          }`}
                        >
                          {msg.from}
                        </b>
                      </div>
                      <div
                        className={`${
                          msg.from === storeInfo.ceo
                            ? ""
                            : styles.messageFromCustomerText
                        }`}
                      >
                        {msg.message}
                      </div>
                    </div>
                  ))}
                </div>
                <div>
                  <form onSubmit={sendMessage} className={styles.messageForm}>
                    <input
                      type="text"
                      className={styles.messageInput}
                      value={message}
                      onChange={handleMessageChange}
                      placeholder="채팅을 입력하세요"
                    />
                    <button type="submit" className={styles.sendButton}>
                      전송
                    </button>
                  </form>
                </div>
              </div>
            </div>
          ) : null}

          {role === "owner" ? (
            <>
              <OpenClose />
              <JiguemOrder />
            </>
          ) : null}
        </div>
      ) : null}
    </div>
  );
};

export default Live;
