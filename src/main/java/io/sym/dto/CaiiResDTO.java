package io.sym.dto;


public class CaiiResDTO {
    private String message;
    private Object result;

    public CaiiResDTO() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
