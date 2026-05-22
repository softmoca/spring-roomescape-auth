package roomescape.exception.client;

import org.springframework.http.HttpStatus;
import roomescape.exception.base.RoomeScapeClientException;

public class ForbiddenException extends RoomeScapeClientException {

    public ForbiddenException(String message) {
        super(message,HttpStatus.FORBIDDEN);
    }
}
