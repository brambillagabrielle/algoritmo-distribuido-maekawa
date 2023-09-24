package br.edu.ifsul.cc.sd2.simulacaomaekawa;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.Address;
import org.jgroups.View;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Maekawa extends ReceiverAdapter {

    private JChannel channel;
    private View view;
    private Mensagem mensagem;

    private int N;
    private Address[] listaMembros;
    private boolean meuVoto;
    private int votosRecebidos;
    private boolean liberado;

    Queue filaEspera = new LinkedList<>();

    void start() throws Exception {

        channel = new JChannel().setReceiver(this);
        channel.connect("Maekawa");

        meuVoto = false;
        votosRecebidos = 0;
        liberado = true;

        iniciaSimulacao();

        channel.close();

    }

    @Override
    public void viewAccepted(View novaView) {

        this.view = novaView;
        listaMembros = view.getMembersRaw();
        N = view.size();

    }

    private void iniciaSimulacao() throws InterruptedException, Exception {

        System.out.println("Sou o processo: " + channel.getAddress());
        System.out.println("Processos atuais: " + view);

        while (true) {

            sleep(10000);

            if (getIndice() % 2 == 0 && N >= 4) {

                if (solicitarEntrada()) {

                    System.out.println("Entrada autorizada pelo quorum!");
                    secaoCritica();

                }

            }

        }

    }

    @Override
    public void receive(Message m) {

        mensagem = (Mensagem) m.getObject();

        Mensagem resposta;

        switch (mensagem.getOperacao()) {

            case "SOLICITAR_ENTRADA":

                if (m.getSrc().equals(channel.address())) {

                    meuVoto = true;
                    votosRecebidos++;

                } else {

                    System.out.println(m.getSrc() + " solicitou a entrada na seção crítica");

                    if (!liberado || meuVoto) {

                        System.out.println("Não posso dar meu voto ainda...");
                        filaEspera.add(m.getSrc());
                        System.out.println("Adicionado à fila de espera: " + filaEspera);

                    } else {

                        System.out.println("Voto que " + m.getSrc() + " pode entrar!");
                        meuVoto = true;
                        resposta = new Mensagem("RESPOSTA_" + mensagem.getOperacao());

                        try {
                            channel.send(m.getSrc(), resposta);
                        } catch (Exception e) {
                            System.out.println("ERRO: " + e);
                        }

                    }

                }

                break;

            case "RESPOSTA_SOLICITAR_ENTRADA":

                votosRecebidos++;
                System.out.println("Recebi um voto que sim do processo: " + m.getSrc());

                break;

            case "INFORMAR_SAIDA":

                if (!filaEspera.isEmpty()) {

                    Address prox = (Address) filaEspera.remove();
                    System.out.println("Próximo processo que vou dar meu voto: " + prox);
                    System.out.println("Fila de espera atual: " + filaEspera);

                    System.out.println("Voto que " + m.getSrc() + " pode entrar!");
                    meuVoto = true;
                    resposta = new Mensagem("RESPOSTA_SOLICITAR_ENTRADA");

                    try {
                        channel.send(prox, resposta);
                    } catch (Exception e) {
                        System.out.println("ERRO: " + e);
                    }

                } else {
                    meuVoto = false;
                }

                break;

        }

    }

    private boolean solicitarEntrada() throws Exception {

        System.out.println("Solicitando a entrada na seção crítica...");

        liberado = false;

        enviarMensagemParaQuorum("SOLICITAR_ENTRADA");

        while (votosRecebidos < (int) Math.sqrt(N) + 1) {
            sleep(5000);
        }

        return true;

    }

    private void enviarMensagemParaQuorum(String operacao) throws Exception {

        mensagem = new Mensagem(operacao);

        int ind = getIndice(), M = (int) Math.sqrt(N), linha = ind / M, coluna = ind % M, indMembro;

        for (int c = 0; c < M; c++) {

            indMembro = linha * M + c;
            channel.send(listaMembros[indMembro], mensagem);
            System.out.println("Enviado para: " + listaMembros[indMembro]);

        }

        for (int l = 0; l < M; l++) {

            if (l != linha) {

                indMembro = l * M + coluna;
                channel.send(listaMembros[indMembro], mensagem);
                System.out.println("Enviado para: " + listaMembros[indMembro]);

            }

        }

    }

    private int getIndice() {

        for (int i = 0; i < listaMembros.length; i++) {

            if (listaMembros[i].equals(this.channel.address())) {
                return i;
            }

        }

        return -1;

    }

    public void secaoCritica() throws InterruptedException, Exception {

        System.out.println("Entrei na seção crítica!");
        escreverArquivo(channel.address() + " entrou da seção critica");

        sleep(10000);

        System.out.println("Saindo da seção crítica...");
        escreverArquivo(channel.address() + " saiu da seção critica");

        enviarMensagemParaQuorum("INFORMAR_SAIDA");

        liberado = true;
        votosRecebidos = 0;
        meuVoto = false;

    }

    private void escreverArquivo(String mensagem) throws FileNotFoundException {

        SimpleDateFormat date = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss");
        String timeStamp = date.format(new Date());

        try (PrintWriter ps = new PrintWriter(
                new FileOutputStream("operacoes-criticas.txt", true))) {
            ps.println(timeStamp + " - " + mensagem);
            ps.flush();
        }

    }

    public Address stringToAddress(String stringAddress) {

        for (Address a : view.getMembersRaw()) {

            if (stringAddress.equals(a.toString())) {
                return a;
            }

        }

        return null;

    }

    public static void main(String[] args) throws Exception {

        Logger jgroupsLogger = Logger.getLogger("org.jgroups");
        jgroupsLogger.setLevel(Level.SEVERE);

        new Maekawa().start();

    }

}
