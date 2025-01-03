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
          BufferedReader reader = new BufferedReader(new InputStreamReader(input));
          PrintWriter writer = new PrintWriter(output, true)
      ) {
        String request;
        while ((request = reader.readLine()) != null) {
          String response = processarTransacao(request);
          writer.println(response);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private String processarTransacao(String mensagemISO) {
      String[] parts = mensagemISO.split(";");
      if (parts.length != 2) {
        return "96;0000000000"; // Código 96: Erro de formato
      }

      String numeroCartao = parts[0];
      double valor;
      try {
        valor = Double.parseDouble(parts[1]);
      } catch (NumberFormatException e) {
        return "96;0000000000"; // Código 96: Erro de formato
      }

      synchronized (Server.class) {
        Cartao cartao = cartoes.get(numeroCartao);
        if (cartao == null) {
          return "05;0000000000"; // Código 05: Cartão inexistente
        }

        synchronized (cartao) {
          if (cartao.debitar(valor)) {
            int nsu = nsuCounter.getAndIncrement();
            return String.format("00;%010d", nsu); // Código 00: Transação aprovada
          } else {
            return "51;0000000000"; // Código 51: Saldo insuficiente
          }
        }
      }
    }
  }
}
