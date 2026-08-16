package edu.austral.ingsis.clifford.command.args.argparser;

import edu.austral.ingsis.clifford.command.Command;
import edu.austral.ingsis.clifford.command.CommandProvider;
import edu.austral.ingsis.clifford.operationResult.OperationError;
import edu.austral.ingsis.clifford.operationResult.OperationResult;
import edu.austral.ingsis.clifford.operationResult.OperationSuccess;

public class CommandParser {
    public static String[] getRequiredArgsForCommand(String command) {
        return new String[0];
    }

    public static OperationResult getCommand(String command) {
        String[] tokens = command.split(" ");
        if (tokens.length == 0) return new OperationError("There is not input str provided!", 120);
        // por ahora no voy a chequear los args
        Command cmd_to_return = null;
        switch (CommandProvider.getCommand(tokens[0])) {
            case OperationError e -> {
                return new OperationError(e.msg(), e.code());
            }
            case OperationSuccess s -> cmd_to_return = (Command) s.data();
        }

        return new OperationSuccess<Command>(
                "Created command \"" + tokens[0] + "\"",
                cmd_to_return
        );
    }
}
