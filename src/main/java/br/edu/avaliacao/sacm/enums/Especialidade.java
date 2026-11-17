package br.edu.avaliacao.sacm.enums;

/** Especialidades atendidas pela clínica. */
public enum Especialidade {
    CLINICO_GERAL("Clínico geral"),
    CARDIOLOGIA("Cardiologia"),
    DERMATOLOGIA("Dermatologia"),
    PEDIATRIA("Pediatria"),
    ORTOPEDIA("Ortopedia");

    private final String descricao;

    Especialidade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
