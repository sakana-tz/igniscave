// EmailConfig.java
package com.evernight.components.emailComponent;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class EmailProperties {
    @Value("${spring.mail.username}")
    private String fromEmail;
}
