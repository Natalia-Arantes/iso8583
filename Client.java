import java.io.*;
import java.net.*;

public class Client {
  public static void main(String[] args) {
    String host = "localhost";
    int port = 12345;

    try (Socket socket = new Socket(host, port)) {
      DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
      DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

      byte[][] transacoesDeTeste = {
          montarMensagemRequisicao("1234567890123456", 20000), // Transação aprovada
          montarMensagemRequisicao("1234567890123456", 150000), // Saldo insuficiente
          montarMensagemRequisicao("0000000000000000", 10000), // Cartão inexistente
          montarMensagemRequisicao("1234567890123456", -5000), // Valor negativo (inválido)
          montarMensagemRequisicao("6543210987654321", 50000), // Transação aprovada para outro cartão
          montarMensagemRequisicao("1234", 10000) // Número de cartão inválido (muito curto)
      };

      for (byte[] mensagem : transacoesDeTeste) {
        dataOutputStream.write(mensagem);

        byte[] resposta = new byte[64];
        dataInputStream.readFully(resposta);

        processarResposta(resposta);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static byte[] montarMensagemRequisicao(String numeroCartao, int valorEmCentavos) {
    byte[] mensagem = new byte[64];

    mensagem[0] = '0'; mensagem[1] = '2'; mensagem[2] = '0'; mensagem[3] = '0';
    String valorString = String.format("%012d", valorEmCentavos);
    System.arraycopy(valorString.getBytes(), 0, mensagem, 4, valorString.length());

    String hora = "104446";
    System.arraycopy(hora.getBytes(), 0, mensagem, 16, hora.length());

    String data = "0512";
    System.arraycopy(data.getBytes(), 0, mensagem, 22, data.length());

    System.arraycopy(numeroCartao.getBytes(), 0, mensagem, 20, numeroCartao.length());

    return mensagem;
  }

  private static void processarResposta(byte[] resposta) {
    if (resposta[0] != '0' || resposta[1] != '2' || resposta[2] != '1' || resposta[3] != '0') {
      System.out.println("Resposta inválida: MTI incorreto");
      return;
    }

    String codigoResposta = new String(resposta, 39, 2);
    switch (codigoResposta) {
      case "00":
        System.out.println("Transação aprovada");
        break;
      case "05":
        System.out.println("Cartão inexistente");
        break;
      case "51":
        System.out.println("Saldo insuficiente");
        break;
      case "96":
        System.out.println("Erro de formato na mensagem");
        break;
      default:
        System.out.println("Código de resposta desconhecido: " + codigoResposta);
    }

    String nsu = new String(resposta, 51, 12).trim();
    System.out.println("NSU: " + nsu);
  }
}
