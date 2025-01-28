import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
  private static final int PORT = 12345;
  private static Map<String, Cartao> cartoes = new HashMap<>();
  private static AtomicInteger nsuCounter = new AtomicInteger(1); // Gerador de NSU

  public static void main(String[] args) {
    cartoes.put("1234567890123456", new Cartao("1234567890123456", "Cliente 1", 1000.0));
    cartoes.put("6543210987654321", new Cartao("6543210987654321", "Cliente 2", 500.0));
    cartoes.put("1111222233334444", new Cartao("1111222233334444", "Cliente 3", 750.0));

    System.out.println("Servidor iniciado na porta " + PORT);

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      while (true) {
        Socket socket = serverSocket.accept();
        new Thread(new ClientHandler(socket)).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try (
          InputStream input = socket.getInputStream();
          OutputStream output = socket.getOutputStream();
          DataInputStream dataInputStream = new DataInputStream(input);
          DataOutputStream dataOutputStream = new DataOutputStream(output)
      ) {
        while (true) {
          byte[] request = new byte[64];
          dataInputStream.readFully(request);

          byte[] response = processarTransacao(request);
          dataOutputStream.write(response);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private byte[] processarTransacao(byte[] mensagemISO) {
      if (mensagemISO[0] != '0' || mensagemISO[1] != '2' || mensagemISO[2] != '0' || mensagemISO[3] != '0') {
        return montarResposta("96", 0, "000000000000"); // Código 96: Erro de formato
      }

      try {
        String numeroCartao = new String(mensagemISO, 32, 16).trim();
        String valorStr = new String(mensagemISO, 4, 12).trim();
        double valor = Double.parseDouble(valorStr) / 100.0;

        Cartao cartao = cartoes.get(numeroCartao);
        if (cartao == null) {
          return montarResposta("05", 0, valorStr); // Código 05: Cartão inexistente
        }

        synchronized (cartao) {
          if (cartao.debitar(valor)) {
            int nsu = nsuCounter.getAndIncrement();
            return montarResposta("00", nsu, valorStr); // Código 00: Transação aprovada
          } else {
            return montarResposta("51", 0, valorStr); // Código 51: Saldo insuficiente
          }
        }
      } catch (Exception e) {
        return montarResposta("96", 0, "000000000000"); // Código 96: Erro de formato
      }
    }

    private byte[] montarResposta(String codigoResposta, int nsu, String valorTransacao) {
      byte[] resposta = new byte[64];
      resposta[0] = '0';
      resposta[1] = '2';
      resposta[2] = '1';
      resposta[3] = '0';

      // Código de resposta no bit 39
      resposta[39] = (byte) codigoResposta.charAt(0);
      resposta[40] = (byte) codigoResposta.charAt(1);

      // Bit 4: Valor original da transação em centavos
      System.arraycopy(valorTransacao.getBytes(), 0, resposta, 4, valorTransacao.length());

      // Bit 12: Hora local da transação
      String horaLocal = "104446";
      System.arraycopy(horaLocal.getBytes(), 0, resposta, 16, horaLocal.length());

      // Bit 13: Data da transação
      String dataTransacao = "0512";
      System.arraycopy(dataTransacao.getBytes(), 0, resposta, 22, dataTransacao.length());

      // Bit 33: Rede transmissora
      String redeTransmissora = "040104";
      System.arraycopy(redeTransmissora.getBytes(), 0, resposta, 26, redeTransmissora.length());

      // NSU no bit 127 (posição 51 no array)
      String nsuString = String.format("%012d", nsu);
      System.arraycopy(nsuString.getBytes(), 0, resposta, 51, nsuString.length());

      return resposta;
    }
  }
}
