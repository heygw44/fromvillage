package com.fromvillage.common.security;

import com.fromvillage.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonSessionInformationExpiredStrategy implements SessionInformationExpiredStrategy {

    private final SecurityResponseWriter responseWriter;

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException, ServletException {
        responseWriter.writeError(event.getResponse(), ErrorCode.AUTH_SESSION_EXPIRED);
    }
}
