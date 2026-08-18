package cnc.cli.args

object ArgsManager {
    private fun setFlags(container : ArgsContainer, arg : String) {
        if (!arg.startsWith("-") && !arg.startsWith("--")) return;
        if (arg.contains("=")) return;

        container.addFlag(arg);
    }

    private fun setOptions(container : ArgsContainer, arg : String) {
        if (!(arg.startsWith("--") && arg.contains("="))) return;
        container.addOption(arg);
    }

    private fun setPositional(container : ArgsContainer, arg : String) {
        if (container.flags.contains(arg)) return;
        if (container.options.contains(arg)) return;

        container.addPositional(arg);
    }
    // gcnc --file=/home/main.cnc | ls --ord=dsc
    // [gcnc, --file=/home/main.cnc, ...]
    fun getArgsContainer(args : List<String>) : ArgsContainer {
        var container = ArgsContainer();

        args.forEach { arg ->
            setFlags(container, arg);
            setOptions(container, arg);
            setPositional(container, arg);
        }

        return container;
    }
}
