import { useNavigate } from 'react-router-dom';
import userStore from 'store/users/userStore';
import styles from './Membership.module.css';
import Header from "components/common/Header";


const Membership = () => {
    const navigate = useNavigate();
    const { getLoginedRole, joinMembership, extendMembership, deactivateMembership } = userStore();

    const role = getLoginedRole();

    const handleJoinClick = () => {
        const confirmed = window.confirm('가입하시겠습니까?');
        if (confirmed) {
            joinMembership(navigate);
        }
    };

    const handleExtendClick = () => {
        const confirmed = window.confirm('연장하시겠습니까?');
        if (confirmed) {
            extendMembership(navigate);
        }
    }

    const handleDeactivateClick = () => {
        const confirmed = window.confirm('정말 탈퇴하시겠습니까?');
        if (confirmed) {
            deactivateMembership(navigate);
        }
    }

    const handleGoBack = () => {
        navigate(-1);
    }

    return (
        <>
            <Header />
            <div className={styles.container}>
                {role.indexOf('vip') === -1 ? <h2 className={styles.heading}>멤버십에 가입하세요!</h2> : <h2 className={styles.heading}>멤버십을 관리하세요!</h2>}
                
                <div>
                    {role.indexOf('vip') === -1 ? <button className={styles.button} onClick={handleJoinClick}>가입하기</button> : <button className={styles.button} onClick={handleExtendClick}>연장하기</button>}
                    {role.indexOf('vip') === -1 ? '' : <button className={styles.button} onClick={handleDeactivateClick}>탈퇴하기</button>}
                </div>
            </div>
        </>
    );
}

export default Membership;