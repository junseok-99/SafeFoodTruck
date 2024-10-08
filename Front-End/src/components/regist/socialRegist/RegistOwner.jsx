import React, { useState, useEffect } from 'react';
import styles from './RegistOwner.module.css';
import useUserStore from 'store/users/userStore';
import axios from 'axios';

const RegistOwner = ({ formData, onFormChange }) => {
  const { nicknameChecked, checkNickname, nicknameTouched, setNicknameTouched, setPasswordMatch, pnChecked, checkPN, bsNumChecked, checkBsNumber } = useUserStore();
  const [maxDate, setMaxDate] = useState('');
  const [bsNumValid, setBsNumValid] = useState(null);

  useEffect(() => {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    setMaxDate(`${yyyy}-${mm}-${dd}`);
  }, []);

  useEffect(() => {
    setPasswordMatch(formData.password === formData.confirmPassword);
  }, [formData.password, formData.confirmPassword, setPasswordMatch]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    onFormChange(name, value); // 부모 컴포넌트로 폼 데이터 전달
    if (name === 'businessNumber') {
      setBsNumValid(null); // 사업자 번호 변경 시 검증 상태 초기화
    }
  };

  const handleNicknameChange = (e) => {
    handleChange(e);
    setNicknameTouched();
  };

  const handleNicknameCheck = () => {
    checkNickname(formData.nickname);
  };

  const handlePNCheck = () => {
    // 전화번호 중복 검사
    checkPN(formData.phoneNumber);
  }

  // 사업자번호 확인 api
  const ntsApiKey = process.env.REACT_APP_NTS_BUSINESS_NUM_API_KEY;
  const data = {
    "b_no": [formData.businessNumber]
  }

  const handleBusNumCheck = async () => {
    try {
      const response = await axios.post(`https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=${ntsApiKey}`,
        data,
        {
          headers: {
            'Content-Type': 'application/json',
            'accept': '*/*',
          }
        });
      if (response.status === 200 && response.data.data[0].b_stt_cd === "01") {
        setBsNumValid(true);
        onFormChange('bsNumValid', true); // bsNumValid 상태를 formData에 저장
        // 사업자 번호 중복 검사
        checkBsNumber(formData.businessNumber);
      } else {
        // 사업자 번호 검증 실패
        setBsNumValid(false);
        onFormChange('bsNumValid', false); // bsNumValid 상태를 formData에 저장
      }
    } catch (error) {
      // 오류
      console.error('사업자 번호 확인 오류:', error)
      setBsNumValid(false);
      onFormChange('bsNumValid', false); // bsNumValid 상태를 formData에 저장
    }
  };

  return (
    <form className={styles.form}>
      <div className={styles.inputContainer}>
        <label>이메일</label>
        <div className={styles.emailContainer}>
          <input type="email" name="email" value={formData.email || ''}  className={styles.emailInput} disabled/>
        </div>
      </div>
      <div className={styles.inputRow}>
        <div className={styles.inputContainer}>
          <label>이름</label>
          <input type="text" name="name" value={formData.name || ''} onChange={handleChange} />
        </div>
        <div className={styles.inputContainer}>
          <label>닉네임</label>
          <div className={styles.nicknameContainer}>
            <input type="text" name="nickname" value={formData.nickname || ''} onChange={handleNicknameChange} className={styles.nicknameInput} />
            <button type="button" className={styles.duplicateButton} onClick={handleNicknameCheck}>중복확인</button>
          </div>
          {nicknameTouched && nicknameChecked === 'Possible' && <p className={styles.hintText}>사용할 수 있는 닉네임이에요</p>}
          {nicknameTouched && nicknameChecked === 'Duplicate' && <p className={styles.errorText}>이미 사용 중인 닉네임입니다</p>}
        </div>
      </div>
      <div className={styles.inputRow}>
        <div className={styles.inputContainer}>
          <label>성별</label>
          <select name="gender" value={formData.gender || ''} onChange={handleChange} className={styles.selectInput}>
            <option value="">선택하세요</option>
            <option value="0">남</option>
            <option value="1">여</option>
          </select>
        </div>
        <div className={styles.inputContainer}>
          <label>생일</label>
          <input type="date" name="birth" value={formData.birth || ''} onChange={handleChange} max={maxDate} />
        </div>
      </div>
      <div className={styles.inputContainer}>
        <label>전화번호</label>
        <div className={styles.emailContainer}>
          <input type="number" name="phoneNumber" value={formData.phoneNumber || ''} onChange={handleChange} className={styles.emailInput} placeholder='숫자만 입력하세요'/>
          <button type="button" className={styles.duplicateButton} onClick={handlePNCheck}>중복확인</button>
        </div>
        {pnChecked === 'Duplicate' && <p className={styles.errorText}>이미 등록된 전화번호입니다</p>}
      </div>
      <div className={styles.inputContainer}>
        <label>사업자 번호</label>
        <div className={styles.emailContainer}>
          <input type="number" name="businessNumber" value={formData.businessNumber || ''} onChange={handleChange} className={styles.emailInput} placeholder='숫자만 입력하세요'/>
          <button type="button" className={styles.duplicateButton} onClick={handleBusNumCheck}>사업자 번호 확인</button>
        </div>
        {bsNumValid === false && <p className={styles.errorText}>사업자 번호가 유효하지 않습니다</p>}
        {bsNumValid === true && bsNumChecked == 'Duplicate' && <p className={styles.errorText}>이미 등록된 사업자 번호입니다</p>}
        {bsNumValid === true && bsNumChecked == 'Possible' && <p className={styles.hintText}>유효한 사업자 번호입니다</p>}
      </div>
    </form>
  );
};

export default RegistOwner;
