package org.sgx.classes.profession.impl;

import org.sgx.classes.profession.ProfessionBehavior;
import org.sgx.classes.profession.ProfessionType;

public final class BlacksmithBehavior implements ProfessionBehavior {

    @Override
    public ProfessionType type() {
        return ProfessionType.BLACKSMITH;
    }
}