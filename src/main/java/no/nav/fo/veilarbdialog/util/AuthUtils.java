//package no.nav.fo.veilarbdialog.util;
//
//import com.nimbusds.jwt.JWTClaimsSet;
//import no.nav.common.auth.context.AuthContextHolder;
//import no.nav.common.auth.context.AuthContextHolderThreadLocal;
//import no.nav.common.auth.context.UserRole;
//import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.Optional;
//
//public class AuthUtils {
//
//    public static String getInnloggetBrukerToken() {
//        return AuthContextHolderThreadLocal
//                .instance().getIdTokenString()
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is missing"));
//    }
//
//    public static VeilederId getInnloggetVeilederIdent() {
//        return AuthContextHolderThreadLocal
//                .instance().getNavIdent()
//                .map(id -> VeilederId.of(id.get()))
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is missing from subject"));
//    }
//
//    public static String hentApplikasjonFraContex(AuthContextHolder authContextHolder) {
//        return authContextHolder.getIdTokenClaims()
//                .flatMap(claims -> getStringClaimOrEmpty(claims, "azp_name")) //  "cluster:team:app"
//                .orElse(null);
//    }
//
//    public static boolean erSystemkallFraAzureAd(AuthContextHolder authContextHolder) {
//        UserRole role = authContextHolder.getRole()
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
//        return UserRole.SYSTEM.equals(role) && harAADRolleForSystemTilSystemTilgang(authContextHolder);
//    }
//
//    private static boolean harAADRolleForSystemTilSystemTilgang(AuthContextHolder authContextHolder) {
//        return authContextHolder.getIdTokenClaims()
//                .flatMap(claims -> {
//                    try {
//                        return Optional.ofNullable(claims.getStringListClaim("roles"));
//                    } catch (ParseException e) {
//                        return Optional.empty();
//                    }
//                })
//                .orElse(emptyList())
//                .contains("access_as_application");
//    }
//
//    public static String getAadOboTokenForTjeneste(AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, DownstreamApi api) {
//        String scope = "api://" + api.cluster() + "." + api.namespace() + "." + api.serviceName() + "/.default";
//        return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, getInnloggetBrukerToken());
//    }
//
//    public static Optional<String> getStringClaimOrEmpty(JWTClaimsSet claims, String claimName) {
//        try {
//            return ofNullable(claims.getStringClaim(claimName));
//        } catch (Exception e) {
//            return empty();
//        }
//    }
//}