package pt.carguru.Utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utilitário de validação de formulários em tempo real.
 *
 * Cada campo é associado a uma {@link Label} de erro (estilo "field-error-label",
 * texto a vermelho) que aparece/desaparece automaticamente conforme o valor do
 * campo é alterado pelo utilizador — sem nunca apagar o que foi escrito.
 *
 * Uso típico:
 * <pre>
 *   FieldValidator fv = new FieldValidator();
 *   fv.validarTexto(campoAno, lblErroAno, v -> {
 *       if (v.isBlank()) return "Ano obrigatório.";
 *       try {
 *           int ano = Integer.parseInt(v.trim());
 *           if (ano < 1900 || ano > LocalDate.now().getYear() + 1) return "Ano inválido.";
 *       } catch (NumberFormatException e) { return "Ano inválido."; }
 *       return null; // null = válido
 *   });
 *   ...
 *   btnSubmeter.setOnAction(e -> {
 *       if (!fv.isValid()) return; // mantém valores, mostra erros já visíveis
 *       // submeter...
 *   });
 * </pre>
 */
public class FieldValidator {

    /** Função de validação: recebe o valor do campo (nunca null para texto) e devolve
     *  a mensagem de erro, ou null/"" se o valor é válido. */
    public interface Regra<T> extends Function<T, String> {}

    private final List<java.util.function.Supplier<Boolean>> verificacoes = new ArrayList<>();

    /**
     * Liga validação em tempo real a um campo de texto.
     * A label de erro é atualizada a cada tecla (textProperty) e também
     * quando o foco é perdido, mas o valor do campo nunca é tocado.
     */
    public void validarTexto(TextInputControl campo, Label lblErro, Regra<String> regra) {
        java.util.function.Supplier<Boolean> validar = () -> aplicar(campo, lblErro, regra.apply(campo.getText() != null ? campo.getText() : ""));
        campo.textProperty().addListener((obs, oldV, newV) -> validar.get());
        campo.focusedProperty().addListener((obs, was, is) -> { if (!is) validar.get(); });
        verificacoes.add(validar);
    }

    /** Liga validação em tempo real a um DatePicker. */
    public void validarData(DatePicker campo, Label lblErro, Regra<java.time.LocalDate> regra) {
        java.util.function.Supplier<Boolean> validar = () -> aplicar(campo, lblErro, regra.apply(campo.getValue()));
        campo.valueProperty().addListener((obs, oldV, newV) -> validar.get());
        // O DatePicker também permite digitar texto diretamente no editor.
        campo.getEditor().focusedProperty().addListener((obs, was, is) -> { if (!is) validar.get(); });
        verificacoes.add(validar);
    }

    /** Liga validação em tempo real a um ComboBox (ex.: combustível, transmissão). */
    public <T> void validarCombo(ComboBox<T> campo, Label lblErro, Regra<T> regra) {
        java.util.function.Supplier<Boolean> validar = () -> aplicar(campo, lblErro, regra.apply(campo.getValue()));
        campo.valueProperty().addListener((obs, oldV, newV) -> validar.get());
        verificacoes.add(validar);
    }

    /** Aplica o resultado da regra: mostra/esconde a mensagem e marca/desmarca o campo, sem alterar o valor. */
    private boolean aplicar(javafx.scene.Node campo, Label lblErro, String mensagemErro) {
        boolean valido = (mensagemErro == null || mensagemErro.isBlank());
        if (lblErro != null) {
            lblErro.setText(valido ? "" : mensagemErro);
            lblErro.setManaged(!valido);
            lblErro.setVisible(!valido);
        }
        if (campo != null) {
            if (valido) campo.getStyleClass().remove("field-invalid");
            else if (!campo.getStyleClass().contains("field-invalid")) campo.getStyleClass().add("field-invalid");
        }
        return valido;
    }

    /**
     * Corre todas as validações registadas (mostrando os respetivos erros, se algum
     * campo estiver inválido) e devolve {@code true} apenas se todos os campos forem válidos.
     * Não limpa nem altera nenhum valor introduzido pelo utilizador.
     */
    public boolean isValid() {
        boolean tudoValido = true;
        for (var v : verificacoes) {
            if (!v.get()) tudoValido = false;
        }
        return tudoValido;
    }
}
