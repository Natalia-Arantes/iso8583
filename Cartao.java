public class Cartao {
  private String numero;
  private String nomeCliente;
  private double saldo;

  public Cartao(String numero, String nomeCliente, double saldo) {
    this.numero = numero;
    this.nomeCliente = nomeCliente;
    this.saldo = saldo;
  }

  public String getNumero() {
    return numero;
  }

  public String getNomeCliente() {
    return nomeCliente;
  }

  public synchronized double getSaldo() {
    return saldo;
  }

  public synchronized boolean debitar(double valor) {
    if (valor > saldo) {
      return false; // Saldo insuficiente
    }
    saldo -= valor;
    return true;
  }

}
