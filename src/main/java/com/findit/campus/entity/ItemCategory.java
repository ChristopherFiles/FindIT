package com.findit.campus.entity;

public enum ItemCategory {
    ELECTRONICS("Elektronik"),
    ID_CARDS("Kartu identitas"),
    KEYS("Kunci"),
    BAGS("Tas"),
    CLOTHING("Pakaian"),
    BOOKS("Buku"),
    ACCESSORIES("Aksesori"),
    OTHER("Lainnya");

    private final String label;

    ItemCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
