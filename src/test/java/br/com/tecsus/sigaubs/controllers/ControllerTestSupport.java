package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static ExtendedModelMap model() {
        return new ExtendedModelMap();
    }

    static RedirectAttributesModelMap redirect() {
        return new RedirectAttributesModelMap();
    }

    static SystemUserDetails admin() {
        return TestDataFactory.adminDetails(1L, "afogados");
    }

    static SystemUserDetails sms() {
        return TestDataFactory.userDetails("sms", "SMS", null, 1L, "afogados", Roles.ROLE_SMS);
    }

    static SystemUserDetails ubsUser(Long basicHealthUnitId) {
        return TestDataFactory.userDetails("ubs", "UBS", basicHealthUnitId, 1L, "afogados", Roles.ROLE_USER);
    }
}
