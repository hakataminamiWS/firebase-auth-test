package controllers

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.typesafe.config.ConfigFactory
import java.io.FileInputStream
import javax.inject._
import org.pac4j.core.exception.http.WithLocationAction
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.credentials.OidcCredentials
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.DataEncrypter
import org.pac4j.play.store.PlayCookieSessionStore
import org.pac4j.play.store.ShiroAesDataEncrypter
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

/** This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents)(
    implicit
    assets: Assets,
    ec: ExecutionContext
) extends BaseController
    with Logging {

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method will be
    * called when the application receives a `GET` request with a path of `/`.
    */

  def index() = Action { implicit request: Request[AnyContent] =>
    logger.debug(
      s"header: authorization : ${request.headers.get("authorization")}"
    )
    Ok(views.html.index())
  }

  // for adding header to wide service worker scope to '/'
  def assetsWithHeader(file: String) =
    Action.async { implicit request: Request[AnyContent] =>
      val path = "/public/javascripts"
      assets.versioned(path, file)(request).map { result =>
        result.withHeaders(("Service-Worker-Allowed", "/"))
      }
    }

  def signWithLine() = Action { implicit request: Request[AnyContent] =>
    val webContext = new PlayWebContext(request)
    val dataEncrypter = new MockDataEncrypter()
    val sessionStore = new PlayCookieSessionStore(dataEncrypter)

    val redirectUrlAfterLogin = "http://localhost:9000/sign-in-ok"

    // set redirect URL
    sessionStore.set(
      webContext,
      Pac4jConstants.REQUESTED_URL,
      redirectUrlAfterLogin
    )
    // url for LINE login
    val location =
      LineOidcClient.oidcClient
        .getRedirectionAction(webContext, sessionStore)
        .get
        .asInstanceOf[WithLocationAction]
        .getLocation

    webContext.supplementResponse(Redirect(location))
  }

  def callbackFromLineLogin() = Action {
    implicit request: Request[AnyContent] =>
      Ok(views.html.callback())
  }

  def idToken() = Action { implicit request: Request[AnyContent] =>
    val webContext = new PlayWebContext(request)
    val dataEncrypter = new MockDataEncrypter() // not encrypt, but pass through
    val sessionStore = new PlayCookieSessionStore(dataEncrypter)

    val optIdToken =
      try {
        LineOidcClient.oidcClient
          .getCredentials(webContext, sessionStore)
          .map(_.asInstanceOf[OidcCredentials])
          .map(_.getIdToken())
          .toScala
      } catch { case e: Exception => None }

    optIdToken match {
      case None =>
        val redirectUrl = "http://localhost:9000"
        val responseJson =
          Json.obj(
            "error" -> "no id token",
            "redirectUrl" -> redirectUrl
          )
        Ok(responseJson)

      case Some(jwt) =>
        val uid = "Line:" + jwt.getJWTClaimsSet.getSubject

        // [optional]
        // set custom claims if want to set user's displayName, photoURL, etc.
        // flow :set custom claims in id token -> read id token in login user's request -> set user's displayName, photoURL, etc.
        val line_name =
          jwt.getJWTClaimsSet.getStringClaim("name").asInstanceOf[Object]
        val line_picture =
          jwt.getJWTClaimsSet.getStringClaim("picture").asInstanceOf[Object]
        // Set admin privilege on the user corresponding to uid.
        val claims =
          HashMap(
            "line_name" -> line_name,
            "line_picture" -> line_picture
          ).asJava

        // exchange id token to firebase token
        val firebaseIdToken =
          FirebaseAuth
            .getInstance(Firebase.firebaseApp)
            .createCustomToken(uid, claims)

        // get redirect URL or default redirect URL
        val redirectUrl = sessionStore
          .get(
            webContext,
            Pac4jConstants.REQUESTED_URL
          )
          .toScala
          .getOrElse("http://localhost:9000")

        val responseJson =
          Json.obj(
            "firebaseIdToken" -> firebaseIdToken,
            "redirectUrl" -> redirectUrl.toString()
          )

        Ok(responseJson)
    }
  }

  def signInOk() = Action { implicit request: Request[AnyContent] =>
    logger.debug(
      s"header: authorization :${request.headers.get("authorization")}"
    )

    val optIdToken = request.headers
      .get("authorization")
      .map(str => str.drop("Bearer ".length))

    optIdToken match {
      case None =>
        logger.debug(s"no idToken in the request header")
        Ok(views.html.signInOk(None))

      case Some(idToken) =>
        try {
          // Verify ID tokens using the Firebase Admin SDK
          // https://firebase.google.com/docs/auth/admin/verify-id-tokens#verify_id_tokens_using_the_firebase_admin_sdk
          // Note: This does not check whether or not the token has been revoked. See: Detect ID token revocation.
          val decodedToken =
            FirebaseAuth
              .getInstance(Firebase.firebaseApp)
              .verifyIdToken(idToken)

          // Verify the ID token while checking if the token is revoked by passing checkRevoked
          // https://firebase.google.com/docs/auth/admin/manage-sessions?hl=ja#detect_id_token_revocation_in_the_sdk
          // as true.
          // val checkRevoked = true
          // val decodedToken = FirebaseAuth
          //   .getInstance(Firebase.firebaseApp)
          //   .verifyIdToken(idToken, checkRevoked)

          val uid = decodedToken.getUid()
          val userName = decodedToken.getName()
          val userPicture = decodedToken.getPicture()

          val userMap = Map(
            "uid" -> uid,
            "userName" -> userName,
            "userPicture" -> userPicture
          )

          Ok(views.html.signInOk(Some(userMap)))

        } catch {
          case e: Exception =>
            // do nothing if idToken is valid or not
            logger.debug(s"occur Exception :${e}")
            Ok(views.html.signInOk(None))
        }
    }
  }

  def signOut() = Action { implicit request: Request[AnyContent] =>
    logger.debug(
      s"header: authorization :${request.headers.get("authorization")}"
    )
    Ok(views.html.signOut())
  }
}

class MockDataEncrypter extends DataEncrypter {
  override def decrypt(encryptedBytes: Array[Byte]): Array[Byte] =
    encryptedBytes
  override def encrypt(rawBytes: Array[Byte]): Array[Byte] = rawBytes

}

object LineOidcClient {

  private val config = ConfigFactory.load()
  private val oidcConfig = new OidcConfiguration()

  private val clientId = config.getString("pac4j.line.channelID")
  oidcConfig.setClientId(clientId)

  private val secret = config.getString("pac4j.line.channelSecret")
  oidcConfig.setSecret(secret)

  oidcConfig.setDiscoveryURI(
    "https://access.line.me/.well-known/openid-configuration"
  )
  oidcConfig.setWithState(true)
  oidcConfig.setUseNonce(true)

  val oidcClient = new OidcClient(oidcConfig)
  oidcClient.setCallbackUrl("http://localhost:9000/callback")
}

object Firebase {
  private val options = FirebaseOptions
    .builder()
    .setCredentials(
      GoogleCredentials.fromStream(
        new FileInputStream("conf/serviceAccountKey.json")
      )
    )
    .build()

  private val apps = FirebaseApp.getApps().asScala.toList
  val firebaseApp =
    apps.headOption.getOrElse(FirebaseApp.initializeApp(options))
}
