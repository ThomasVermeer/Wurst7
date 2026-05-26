/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.TextFieldSetting;

import java.util.ArrayList;
import java.util.List;

@SearchTags({"chat command pattern", "command pattern", "command sequence", "macro",
        "chat macro", "command macro", "auto command"})
public final class ChatCommandPatternHack extends Hack implements UpdateListener
{
    private final TextFieldSetting patternSetting = new TextFieldSetting("Pattern",
            "Format: delay:command, delay:command, ...\n"
                    + "Example: 3:/sell all, 5:/home, 10:/spawn",
            "3:/say Hello, 6:/home");

    private final CheckboxSetting loop = new CheckboxSetting("Loop",
            "Repeat the pattern forever", true);

    private final List<CommandEntry> pattern = new ArrayList<>();
    private int step = 0;
    private long nextExecutionTime = 0;

    public ChatCommandPatternHack()
    {
        super("ChatCommandPattern");
        setCategory(Category.CHAT);

        addSetting(patternSetting);
        addSetting(loop);
    }

    @Override
    protected void onEnable()
    {
        parsePattern();
        step = 0;
        nextExecutionTime = System.currentTimeMillis();
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);
        pattern.clear();
    }

    private void parsePattern()
    {
        pattern.clear();
        String input = patternSetting.getValue().trim();

        if (input.isEmpty())
            return;

        for (String entry : input.split(","))
        {
            String[] parts = entry.trim().split(":", 2);
            if (parts.length != 2)
                continue;

            try
            {
                int delay = Integer.parseInt(parts[0].trim());
                String command = parts[1].trim();

                if (delay > 0 && !command.isEmpty())
                    pattern.add(new CommandEntry(delay, command));
            }
            catch (Exception ignored) {}
        }
    }

    @Override
    public void onUpdate()
    {
        if (pattern.isEmpty() || System.currentTimeMillis() < nextExecutionTime)
            return;

        CommandEntry entry = pattern.get(step);

        String rawCommand = entry.command.startsWith("/")
                ? entry.command.substring(1)
                : entry.command;

        if (MC.getConnection() != null)
            MC.getConnection().sendCommand(rawCommand);

        nextExecutionTime = System.currentTimeMillis() + (entry.delay * 1000L);

        step = (step + 1) % pattern.size();

        if (step == 0 && !loop.isChecked())
            setEnabled(false);
    }

    private record CommandEntry(int delay, String command) {}

    @Override
    public String getRenderName()
    {
        return "ChatCommandPattern [" + pattern.size() + "]";
    }
}