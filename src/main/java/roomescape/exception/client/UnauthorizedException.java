package roomescape.exception.client;

import org.springframework.http.HttpStatus;
import roomescape.exception.base.RoomeScapeClientException;

public class UnauthorizedException extends RoomeScapeClientException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
