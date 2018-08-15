package com.google.devtools.kythe.platform.shared;

import com.google.devtools.kythe.platform.shared.KytheMetadataLoader;
import com.google.devtools.kythe.platform.shared.Metadata;

import javax.annotation.Nullable;

final class AutoValue_KytheMetadataLoader_RuleXorError extends KytheMetadataLoader.RuleXorError {

    private final Metadata.Rule rule;
    private final String error;

    AutoValue_KytheMetadataLoader_RuleXorError(
            @Nullable Metadata.Rule rule,
            @Nullable String error) {
        this.rule = rule;
        this.error = error;
    }

    @Nullable
    @Override
    Metadata.Rule rule() {
        return rule;
    }

    @Nullable
    @Override
    String error() {
        return error;
    }

    @Override
    public String toString() {
        return "RuleXorError{"
               + "rule=" + rule + ", "
               + "error=" + error
               + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof KytheMetadataLoader.RuleXorError) {
            KytheMetadataLoader.RuleXorError that = (KytheMetadataLoader.RuleXorError) o;
            return ((this.rule == null) ? (that.rule() == null) : this.rule.equals(that.rule()))
                   && ((this.error == null) ? (that.error() == null) : this.error.equals(that.error()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (rule == null) ? 0 : this.rule.hashCode();
        h *= 1000003;
        h ^= (error == null) ? 0 : this.error.hashCode();
        return h;
    }

}
