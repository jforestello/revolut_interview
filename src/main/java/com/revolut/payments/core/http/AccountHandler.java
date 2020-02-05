package com.revolut.payments.core.http;

import com.revolut.payments.JsonUtils;
import com.revolut.payments.controllers.AccountController;
import com.revolut.payments.core.Injector;
import com.revolut.payments.core.RequestParser;
import com.revolut.payments.dto.ResponseDTO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class AccountHandler implements HttpHandler {
    private RequestParser parser;

    public AccountHandler(final RequestParser parser) {
        this.parser = parser;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final AccountController controller = Injector.getInjector().getInstance(AccountController.class);
        final ResponseDTO response = controller.handle(parser.parse(exchange));
        if (response == null) {
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
            return;
        }
        final String responseBody = JsonUtils.Gson.toJson(response.getBody());
        exchange.sendResponseHeaders(response.getHttpStatus(), responseBody.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(responseBody.getBytes());
        output.flush();
        exchange.close();
    }
}
