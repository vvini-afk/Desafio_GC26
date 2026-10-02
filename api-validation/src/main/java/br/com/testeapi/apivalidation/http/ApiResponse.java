package br.com.testeapi.apivalidation.http;

public record ApiResponse(int statusCode, String body) {
    public boolean deuCerto() {
        return statusCode >= 200 && statusCode < 300;
    }
}
