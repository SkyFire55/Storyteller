package com.ylinor.storyteller;

import com.ylinor.storyteller.action.DialogAction;
import com.ylinor.storyteller.action.KillCountAction;
import com.ylinor.storyteller.action.MiscellaneousAction;
import com.ylinor.storyteller.action.ObjectiveAction;
import com.ylinor.storyteller.data.ActionEnum;
import com.ylinor.storyteller.data.beans.*;

import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class BookGenerator {
    @Inject
    private Game game;
    @Inject
    private DialogAction dialogAction;
    @Inject
    private ObjectiveAction objectiveAction;
    @Inject
    private KillCountAction killCountAction;
    @Inject
    private MiscellaneousAction miscellaneousAction;

    /**
     * Create a bookview from a dialog
     * @param dialog Dialog data
     * @return BookView to display
     */
    public BookView generateDialog(DialogBean dialog){
        BookView.Builder bookviewBuilder = BookView.builder();
        for (PageBean pageBean : dialog.getPages()) {
            bookviewBuilder.addPage(generatePage(pageBean, dialog.getTrigger()));
        }
        return bookviewBuilder.build();
    }

    /**
     * Generate the text to print inside a page
     * @param page Page data
     * @return Text to display (with optional buttons)
     */
    private Text generatePage(PageBean page, List<String> npcNames){
        Text.Builder text = Text.builder(page.getMessage() + "\n");
        if (!page.getButtonBeanList().isEmpty()) {
            List<ButtonBean> buttons = page.getButtonBeanList();
            for (ButtonBean buttonBean : buttons) {
                text.append(generateButton(buttonBean, npcNames));
            }
        }
        return text.build();
    }

    /**
     * Create the default BookView
     * @param player Player to address to
     * @return BookView to display
     */
    public BookView generateDefaultBook(Player player){
        BookView.Builder bookviewBuilder = BookView.builder();
        Text.Builder text = Text.builder("Salutations, " + player.getName() + ".");
        bookviewBuilder.addPage(text.build());
        return bookviewBuilder.build();
    }

    /**
     * Generate a button that will commit an action
     * @param buttonBean Button data
     * @return Printed button
     */
    private Text generateButton(ButtonBean buttonBean, List<String> npcNames) {
        Text.Builder textBuilder = Text.builder(buttonBean.getText());
        Optional<TextColor> textColor = game.getRegistry().getType(TextColor.class,buttonBean.getColor().toUpperCase());
        if (textColor.isPresent()) {
            textBuilder.color(textColor.get());
        }
        List<ActionBean> actions = buttonBean.getActions();
        Map<ActionEnum, String> effectiveActions = new HashMap<>();
        for(ActionBean action: actions) {
            Storyteller.getLogger().info(action.getName());
            effectiveActions.put(ActionEnum.valueOf(action.getName()), action.getArg());
        }
        // Concatenate NPC names
        String npcNamesString = "";
        for (String npcName : npcNames) {
            npcNamesString += npcName;
        }
        final String npcNameStringFinal = npcNamesString;
        // Set button action
        textBuilder.onClick(TextActions.executeCallback(commandSource-> {
            for (Map.Entry<ActionEnum, String> effectiveAction : effectiveActions.entrySet()) {
                switch (effectiveAction.getKey()) {
                    case OPEN_DIALOG:
                        changeDialog((Player)commandSource,Integer.parseInt(effectiveAction.getValue()));
                        break;
                    case EXECUTE_COMMAND:
                        miscellaneousAction.executeCommand((Player)commandSource, effectiveAction.getValue());
                        break;
                    case TELEPORT:
                        miscellaneousAction.teleport((Player)commandSource, effectiveAction.getValue());
                        break;
                    case GIVE_ITEM:
                        miscellaneousAction.giveItem((Player)commandSource, effectiveAction.getValue());
                        break;
                    case REMOVE_ITEM:
                        miscellaneousAction.removeItem((Player)commandSource, effectiveAction.getValue());
                        break;
                    case SET_OBJECTIVE:
                        objectiveAction.setObjective((Player)commandSource, effectiveAction.getValue());
                        break;
                    case START_KILL_COUNT:
                        killCountAction.startKillCount((Player)commandSource, npcNameStringFinal, effectiveAction.getValue());
                        break;
                    case STOP_KILL_COUNT:
                        killCountAction.stopKillCount((Player)commandSource, npcNameStringFinal, effectiveAction.getValue());
                        break;
                }
            }
        }));
        return textBuilder.build();
    }

    /**
     * Change the current dialog to a given dialog
     * @param source Player to show dialog to
     * @param dialogIndex Index of the dialog to show
     */
    private void changeDialog(Player source, int dialogIndex) {
        Optional<DialogBean> dialogBeanOptional = dialogAction.getDialog(dialogIndex);
        if(dialogBeanOptional.isPresent()){
            source.sendBookView(generateDialog(dialogBeanOptional.get()));
        } else {
            source.sendMessage(Text.builder("The dialog at the index : "+ dialogIndex + " cannot be loaded.").color(TextColors.RED).build());
        }
    }
}
