package edu.austral.ingsis.clifford.command.args;

import java.util.*;

public class ArgsContainer {
    private Set<String> flags; // --recursive
    private List<String> options; // --ord=asd
    private List<String> positional; // /home/usr

    public ArgsContainer() {
        flags = new HashSet<String>();
        options = new ArrayList<String>();
        positional = new LinkedList<String>();
    }

    public void addFlag(String flag) {
        this.flags.add(flag);
    }

    public void addOption(String option) {
        this.options.add(option);
    }

    public void addPositional(String positional) {
        this.positional.add(positional);
    }

    public void setFlags(final Set<String> flags) {
        this.flags = flags;
    }

    public void setOptions(final List<String> options) {
        this.options = options;
    }

    public void setPositional(final List<String> positional) {
        this.positional = positional;
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public boolean hasOption(String option) {
        return options.contains(option);
    }

    public boolean hasAtLeastPositional(Integer many) {
        return positional.size() >= many;
    }

    public boolean hasNPositional(Integer n) {
        return positional.size() == n;
    }

    public String getPositionalAt(final int position) {
        return positional.get(position);
    }

    public Set<String> getFlags() {
        return flags;
    }

    public List<String> getOptions() {
        return options;
    }

    public List<String> getPositional() {
        return positional;
    }
}
