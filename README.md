# firebase-auth-test
Sign in (LINE Login) test with Firebase Authentication, Play Framework 2.8.15 and pac4j v5.4.

# Flow
```mermaid
sequenceDiagram
  autonumber
  Browser->>+This App: /sign-in-with-line
  This App->>-Browser: redirect for LINE Login
  Browser->>+Line Platform: Access the LINE Login
  Line Platform->>-Browser: Authentication
  Browser->>+This App: Redirect with Authentication / Authorization code 
  This App->>+Line Platform: Request LINE Id Token
  Line Platform->>-This App: Id token
  Note over This App: create Firebase custom token from LINE Id token
  This App->>-Browser: Return Firebase custom token
  Browser->>+Firebase Authentication: Sign in using custom token
  Firebase Authentication->>-Browser: Firebase Id token
  Browser->>This App: Access with Firebase Id token in request header.

```

# Link
 - [Firebase Authentication](https://firebase.google.com/docs/auth?hl=ja)
 - [LINE Login v2.1](https://developers.line.biz/ja/reference/line-login/)
 - [Play Framework 2.8.15](https://www.playframework.com/documentation/2.8.x/ScalaHome)
 - [pac4j v5.4](https://www.pac4j.org/docs/index.html)

# License
MIT
