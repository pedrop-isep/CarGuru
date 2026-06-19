package pt.carguru.Utils;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Acelera a velocidade do scroll (roda do rato / trackpad) em todos os
 * ScrollPane da aplicação. Por padrão o JavaFX faz um scroll muito lento
 * e pouco responsivo; este utilitário multiplica o deslocamento para que
 * a navegação pelas páginas fique mais fluida.
 */
public final class ScrollSpeedUtil {

    private ScrollSpeedUtil() {}

    /** Quanto maior, mais rápido o scroll. Ajustável conforme sensação desejada. */
    private static final double VELOCIDADE = 2.6;

    /**
     * Percorre toda a árvore de nós a partir da raiz dada e aplica
     * a aceleração de scroll a cada ScrollPane encontrado (incluindo
     * os que estão dentro de Tabs de um TabPane).
     */
    public static void aplicar(Parent raiz) {
        if (raiz == null) return;
        percorrer(raiz);
    }

    private static void percorrer(Node node) {
        if (node instanceof ScrollPane sp) {
            acelerar(sp);
        }
        if (node instanceof TabPane tabPane) {
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getContent() != null) percorrer(tab.getContent());
            }
        }
        if (node instanceof Parent parent) {
            for (Node filho : parent.getChildrenUnmodifiable()) {
                percorrer(filho);
            }
        }
    }

    private static void acelerar(ScrollPane scrollPane) {
        scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;

            // Se o evento ocorreu dentro de um ScrollPane mais interno (ex.: o
            // ScrollPane de uma Tab, dentro do ScrollPane geral da página),
            // deixa esse ScrollPane interno tratar o scroll e não o consumas aqui.
            if (event.getTarget() instanceof Node alvo) {
                ScrollPane maisInterno = encontrarScrollPaneAncestral(alvo);
                if (maisInterno != null && maisInterno != scrollPane) return;
            }

            double alturaConteudo = scrollPane.getContent() != null
                    ? scrollPane.getContent().getBoundsInLocal().getHeight()
                    : 1;
            double alturaViewport = scrollPane.getViewportBounds() != null
                    ? scrollPane.getViewportBounds().getHeight()
                    : 1;
            double alturaScrollavel = Math.max(alturaConteudo - alturaViewport, 1);
            double deslocamento = (event.getDeltaY() * VELOCIDADE) / alturaScrollavel;
            scrollPane.setVvalue(scrollPane.getVvalue() - deslocamento);
            event.consume();
        });
    }

    /** Devolve o ScrollPane mais próximo (ancestral mais interno) a partir do nó dado, ou null se não houver nenhum. */
    private static ScrollPane encontrarScrollPaneAncestral(Node node) {
        Node atual = node;
        while (atual != null) {
            if (atual instanceof ScrollPane sp) return sp;
            atual = atual.getParent();
        }
        return null;
    }
}
