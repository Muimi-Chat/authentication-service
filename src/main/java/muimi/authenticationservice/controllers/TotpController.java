package muimi.authenticationservice.controllers;

import jakarta.persistence.EntityNotFoundException;
import muimi.authenticationservice.responsemodel.CreateTotpResponse;
import muimi.authenticationservice.responsemodel.VerifyEmailVerificationTokenResponse;
import muimi.authenticationservice.responsemodel.VerifyTotpCodeResponse;
import muimi.authenticationservice.services.GenerateTotpQrService;
import muimi.authenticationservice.services.GenerateTotpSecretService;
import muimi.authenticationservice.services.VerifyTotpCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TotpController {
    private static final Logger log = LoggerFactory.getLogger(TotpController.class);

    @Value("${application.api-auth-key}")
    private String apiAuthKey;

    @Autowired
    private GenerateTotpQrService generateTotpQrService;

    @Autowired
    private GenerateTotpSecretService generateTotpSecretService;

    @Autowired
    private VerifyTotpCodeService verifyTotpCodeService;

    @PostMapping(path="/totp/generate-token")
    public @ResponseBody ResponseEntity<CreateTotpResponse> generateToken(
            @RequestHeader(name="Authorization") String authHeader,
            @RequestParam String accountID,
            @RequestParam String username
    ) {
        if (isBadApiKey(authHeader)) {
            CreateTotpResponse response = new CreateTotpResponse("BAD_API_KEY", null, null);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            String secret = generateTotpSecretService.generateTotp(accountID);
            String qrCodeUri = generateTotpQrService.generateQrCodeUri(secret, username);
            CreateTotpResponse response = new CreateTotpResponse("SUCCESS", secret, qrCodeUri);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            CreateTotpResponse response = new CreateTotpResponse("SERVER_ERROR", null, null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path="/totp/verify-code")
    public @ResponseBody ResponseEntity<VerifyTotpCodeResponse> verifyCode(
            @RequestHeader(name="Authorization") String authHeader,
            @RequestParam String accountID,
            @RequestParam String code
    ) {
        if (isBadApiKey(authHeader)) {
            VerifyTotpCodeResponse response = new VerifyTotpCodeResponse("BAD_API_KEY", false);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            boolean valid = verifyTotpCodeService.verifyCode(accountID, code);
            VerifyTotpCodeResponse response = new VerifyTotpCodeResponse("SUCCESS", valid);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            log.error(e.getMessage());
            VerifyTotpCodeResponse response = new VerifyTotpCodeResponse("NO_TOKEN_SET", false);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }  catch (Exception e) {
            log.error(e.getMessage());
            VerifyTotpCodeResponse response = new VerifyTotpCodeResponse("SERVER_ERROR", false);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isBadApiKey(String apiKey) {
        return !apiKey.equals(apiAuthKey);
    }
}
