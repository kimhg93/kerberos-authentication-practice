# kerberos-authentication-practice
Windows 로그인 유저의 SSO 처리를 위한 Kerberos 인증 예제

kerberos 인증을 구현 하는 과정에서 국내 레퍼런스가 거의 전무하여 기록을 남김

### kerberos 인증을 적용하면?
Windows 인트라넷 환경에서 웹 어플리케이션 접속 시 별도의 로그인 절차를 필요로 하지 않는다. 

VPN 사용시에도 동일하게 로그인이 불필요 하며, 인트라넷 업무 시스템의 로그인 절차를 제거 할 수 있다.

### 개발 환경
Java: 11.0.2

Build: Gradle 6.8.3

Spring Boot: 2.7.11


### 사전 설정
가장 중요한 부분이다. 인증 구현 시 실제 코드의 양은 많지 않으며, 인증 과정에 대한 이해와 사전 설정이 가장 중요.

보통 개발자가 직접 컨트롤 할 수 없는 영역인데다, 사전 설정의 문제로 인증이 실패한다면 디버깅이 상당히 어렵움

1. 서비스계정 생성 (해당 서비스 계정으로 webapp 이 실행되어야 한다)
2. SPN 등록(Domain Controller에서 수행)
 - 서비스 계정을 SPN 으로 등록해야 한다.
 - "setspn -s http/\<domain\> \<username\>"
3. Keytab 생성(Domain Controller에서 수행)
 - 생성 명령어의 각각의 옵션이 중요하며 오류가 가장 잘 발생하는 부분
 - "ktpass /out \<keytab-file-name\> /princ \<SPN\>/\<FQDN\>@\<REALM\> /mapuser \<username\> /pass \<password\> /ptype KRB5_NT_PRINCIPAL /crypto all  /kvno 0"

### 인증 절차
1. 클라이언트가 인증을 필요로 하는 웹 어플리케이션에 접속
2. Spring Security에서 인증되지 않은 사용자의 접근으로 인한 예외(Exception)가 발생
3. SpnegoEntryPoint에서 WWW-Authenticate 헤더와 함께 401 Unauthorized 응답
4. 클라이언트의 브라우저는 인증 헤더를 생성하여 서버로 전달 (인증 헤더에는 클라이언트 PC의 Kerberos 티켓 정보, 보안 컨텍스트 정보 등이 포함됨)
5. SunJaasKerberosTicketValidator가 HTTP 요청 헤더에서 인증 티켓 추출 및 검증
6. kerberosServiceAuthenticationProvider기 KDC(Key Distribution Center)로 클라이언트 티켓 검증 및 서비스 티켓 발급 요청
7. KDC는 인증 정보가 유효한 경우 서비스 티켓 발급
8. KerberosAuthenticationProvider는 서비스 티켓에 포함된 Principal 정보를 토대로 Authentication 객체 생성
9. 인증 완료


### 참고링크
https://docs.spring.io/spring-security-kerberos/docs/current/reference/html/index.html

https://tomcat.apache.org/tomcat-9.0-doc/windows-auth-howto.html

http://code-addict.pl/spring-security-kerberos/


