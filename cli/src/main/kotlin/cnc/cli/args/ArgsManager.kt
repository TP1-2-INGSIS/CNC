package cnc.cli.args

object ArgsManager {
    private fun setFlags(ArgsContainer container, String arg) {
        if (!arg.startsWith("-") && !arg.startsWith("--")) return;
        if (arg.contains("=")) return;

        container.addFlag(arg);
    }

    private fun setOptions(ArgsContainer container, String arg) {
        if (!(arg.startsWith("--") && arg.contains("="))) return;
        container.addOption(arg);
    }

    private fun setPositional(ArgsContainer container, String arg) {
        if (container.getFlags().contains(arg)) return;
        if (container.getOptions().contains(arg)) return;

        container.addPositional(arg);
    }

    fun getArgsContainer(String[] args) : ArgsContainer {
        var container = ArgsContainer();

        for (String arg : args) {
            setFlags(container, arg);
            setOptions(container, arg);
            setPositional(container, arg);
        }

        return container;
    }
}
