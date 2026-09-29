package com.mekexnihilo.registry;

import com.mekexnihilo.MekExNihilo;
import mekanism.api.text.ILangEntry;

/** Translation keys used by the machine descriptions and their container titles. */
public enum MekExNihiloLang implements ILangEntry {
    DESCRIPTION_ELECTRIC_SIEVE("description", "electric_sieve"),
    DESCRIPTION_SIEVE_FACTORY("description", "sieve_factory"),
    ELECTRIC_SIEVE("container", "electric_sieve");

    private final String key;

    MekExNihiloLang(String type, String path) {
        this.key = type + "." + MekExNihilo.MOD_ID + "." + path;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}
