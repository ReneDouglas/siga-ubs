package br.com.tecsus.sigaubs.dtos;

public record ResultadoOperacao<T>(boolean ok, String codigo, String mensagem, T valor) {

    public static <T> ResultadoOperacao<T> sucesso(T valor) {
        return new ResultadoOperacao<>(true, null, null, valor);
    }

    public static ResultadoOperacao<Void> sucessoSemValor() {
        return new ResultadoOperacao<>(true, null, null, null);
    }

    public static ResultadoOperacao<Void> sucesso(String mensagem) {
        return new ResultadoOperacao<>(true, null, mensagem, null);
    }

    public static <T> ResultadoOperacao<T> sucesso(String mensagem, T valor) {
        return new ResultadoOperacao<>(true, null, mensagem, valor);
    }

    public static <T> ResultadoOperacao<T> falha(String mensagem) {
        return new ResultadoOperacao<>(false, null, mensagem, null);
    }

    public static <T> ResultadoOperacao<T> falha(String codigo, String mensagem) {
        return new ResultadoOperacao<>(false, codigo, mensagem, null);
    }

    public boolean falhou() {
        return !ok;
    }

    public boolean sucesso() {
        return ok;
    }
}
