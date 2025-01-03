import java.io.*;
import java.net.*;

public class Client {
  public static void main(String[] args) {
    String host = "localhost";
    int port = 12345;

    try (Socket socket = new Socket(host, port)) {
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      String[][] transacoesDeTeste = {
          {"1234567890123456", "200.0", "Transação aprovada (Saldo suficiente)"},
          {"1234567890123456", "1500.0", "Saldo insuficiente"},
          {"0000000000000000", "100.0", "Cartão inexistente"},
          {"1234567890123456", "abc", "Erro de formato na mensagem (valor inválido)"},
          {"1234567890123456", "", "Erro de formato na mensagem (valor ausente)"},
          {"1234567890123456", "-50.0", "Valor negativo (inválido)"},
          {"1234567890123456", "1000.0", "Transação aprovada (Saldo exato que zera o saldo)"},
          {"1234", "100.0", "Número de cartão inválido (muito curto)"},
          {"1234567890123456", "50.0", "Transação repetida para testar consistência"},
          {"6543210987654321", "500.0", "Transação aprovada (Usa outro cartão com saldo suficiente)"},
          {"6543210987654321", "0.0", "Transação com valor zero (inválido ou ignorado)"}
      };

      for (String[] transacao : transacoesDeTeste) {
        String numeroCartao = transacao[0];
        String valor = transacao[1];
        String descricao = transacao[2];

        String mensagem = numeroCartao + ";" + valor;
        System.out.println("Simulando: " + descricao);
        System.out.println("Enviando: " + mensagem);
        writer.println(mensagem);
        String resposta = reader.readLine();
        System.out.println("Resposta: " + resposta);
        System.out.println("----------");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
