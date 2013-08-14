package net.aufdemrand.denizen.scripts.commands.core;

import java.util.ArrayList;
import java.util.List;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.queues.ScriptQueue;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.objects.Duration;
import net.aufdemrand.denizen.objects.aH;
import net.aufdemrand.denizen.scripts.queues.core.TimedQueue;
import net.aufdemrand.denizen.utilities.debugging.dB;


public class QueueCommand extends AbstractCommand {

    private enum Action { CLEAR, SET, DELAY, PAUSE, RESUME }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        Action action = null;
        Duration delay = null;

        List<ScriptQueue> queues = new ArrayList<ScriptQueue>();

        // Use current queue if none specified.
        queues.add(scriptEntry.getResidingQueue());

        for (String arg : scriptEntry.getArguments()) {

            if (aH.matchesArg("CLEAR, SET, PAUSE, RESUME", arg))
                action = Action.valueOf(aH.getStringFrom(arg).toUpperCase());

            else if (aH.matchesValueArg("DELAY", arg, aH.ArgumentType.Duration)) {
                action = Action.DELAY;
                delay = aH.getDurationFrom(arg);
            }

            // queue: argument should be optional in this command
            else {
                queues.clear();
                for (String queueName : aH.getListFrom(arg)) {
                    try {
                        queues.add(ScriptQueue._getExistingQueue(queueName));
                    } catch (Exception e) {
                        // must be null, don't add
                    }
                }

            }
        }

        // If no queues have been added, assume 'residing queue'
        if (queues.isEmpty()) queues.add(scriptEntry.getResidingQueue());

        // Check required args
        if (action == null)
            throw new InvalidArgumentsException("Must specify an action. Valid: CLEAR, SET, DELAY, PAUSE, RESUME");

        if (action == Action.DELAY && delay == null)
            throw new InvalidArgumentsException("Must specify a delay.");

        // Stash args in ScriptEntry for use in execute()
        scriptEntry.addObject("queues", queues)
                .addObject("action", action)
                .addObject("delay", delay);
    }

    @SuppressWarnings({ "incomplete-switch", "unchecked" })
    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        List<ScriptQueue> queues = (List<ScriptQueue>) scriptEntry.getObject("queues");
        Action action = (Action) scriptEntry.getObject("action");
        Duration delay = (Duration) scriptEntry.getObject("duration");

        // Debugger
        dB.report(getName(), aH.debugObj("Queues", queues.toString())
                + aH.debugObj("Action", action.toString())
                + (action != null && action == Action.DELAY ? delay.debug() : ""));

        switch (action) {

            case CLEAR:
                for (ScriptQueue queue : queues)
                    queue.clear();
                return;

            case PAUSE:
                for (ScriptQueue queue : queues)
                if (queue instanceof TimedQueue)
                    ((TimedQueue) queue).setPaused(true);
                return;

            case RESUME:
                for (ScriptQueue queue : queues)
                    if (queue instanceof TimedQueue)
                        ((TimedQueue) queue).setPaused(false);
                return;

            case DELAY:
                for (ScriptQueue queue : queues)
                    if (queue instanceof TimedQueue)
                        ((TimedQueue) queue).delayFor(delay);
                return;

        }

    }

}

