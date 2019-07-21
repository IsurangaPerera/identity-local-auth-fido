/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.fido2.endpoint.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.api.FidoApi;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authenticator.fido2.core.WebAuthnService;
import org.wso2.carbon.identity.application.authenticator.fido2.dto.RegistrationRequest;
import org.wso2.carbon.identity.application.authenticator.fido2.endpoint.exception.BadRequestException;
import org.wso2.carbon.identity.application.authenticator.fido2.endpoint.exception.InternalServerErrorException;
import org.wso2.carbon.identity.application.authenticator.fido2.exception.FIDO2AuthenticatorException;
import org.wso2.carbon.identity.application.authenticator.fido2.exception.FIDO2AuthenticatorServerException;
import org.wso2.carbon.identity.application.authenticator.fido2.util.Either;
import org.wso2.carbon.identity.application.authenticator.fido2.util.FIDOUtil;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;


public class FidoApiServiceImpl implements FidoApi {

    private static Log log = LogFactory.getLog(FidoApiServiceImpl.class);

    private static final String EQUAL_OPERATOR = "=";

    @Override
    public String meWebauthnStartRegistrationPost(String username) throws FIDO2AuthenticatorException {

        try {
            WebAuthnService service = new WebAuthnService();
            if(username.contains(EQUAL_OPERATOR)) {
                username = URLDecoder.decode(username.split(EQUAL_OPERATOR)[1], IdentityCoreConstants.UTF_8);
            }
            Either<String, RegistrationRequest> result = service.startRegistration(username);
            if (result.isRight()) {
                return FIDOUtil.writeJson(result.right().get());
            } else {
                throw new FIDO2AuthenticatorException(result.left().get());
            }
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public String meWebauthnFinishRegistrationPost(String response) {

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("Received finish registration response: {0}", response));
        }
        try {
            // WebAuthnService has been initiated multiple times instead of having a single instance to prevent
            // SecurityPrivilegeException at the time of bean instantiation. This can be reverted once duplicated
            // jackson dependencies are removed from CXF runtime
            WebAuthnService service = new WebAuthnService();
            service.finishRegistration(response);
        } catch (FIDO2AuthenticatorServerException ex) {
            throw new InternalServerErrorException(ex);
        } catch (FIDO2AuthenticatorException | IOException e) {
            throw new BadRequestException(e);
        }
        return response;
    }

    @Override
    public void meWebauthnCredentialIdDelete(String credentialId) {

        try {
            WebAuthnService service = new WebAuthnService();
            service.deregisterCredential(credentialId);
        } catch (IOException e) {
            if(log.isDebugEnabled()) {
                log.debug("Failed to write response as JSON", e);
            }
            throw new BadRequestException(e);
        }
    }

    @Override
    public String meWebauthnGet(String username) {

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("fetching device metadata for the username: {0}", username));
        }
        try {
            WebAuthnService service = new WebAuthnService();
            if(username.contains(EQUAL_OPERATOR)) {
                username = URLDecoder.decode(username.split(EQUAL_OPERATOR)[1], IdentityCoreConstants.UTF_8);
            }
            return FIDOUtil.writeJson(service.getDeviceMetaData(username));
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new BadRequestException(e);
        }
    }
}

