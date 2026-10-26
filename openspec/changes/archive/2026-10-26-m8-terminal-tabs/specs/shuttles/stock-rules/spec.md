# Spec Delta

## MODIFIED Requirements

### Requirement: Rule editing in the terminal
The Storage Terminal's item popup SHALL have a Rule tab with an "Order when below" (the minimum) and a "Fill up to" (the maximum) one line stepper, Save and Remove. Save SHALL set the rule for the popup's item; Remove SHALL delete it and SHALL be greyed out while the item has no rule. Save SHALL be greyed out, with the reason as its tooltip and in the tab, while the minimum is not below the maximum. Items with a rule SHALL be marked in the stock grid and their tooltip SHALL show the rule and its state.

#### Scenario: Set a rule
- **WHEN** the player right clicks sticks, picks the Rule tab, sets Order when below to 8 and Fill up to to 16 and presses Save
- **THEN** the sticks rule exists and the sticks entry shows a marker and "keep 8 to 16" in its tooltip

#### Scenario: Remove a rule
- **WHEN** the player opens the Rule tab for sticks, which have a rule, and presses Remove
- **THEN** the sticks rule is gone

#### Scenario: Minimum not below maximum
- **WHEN** the Rule tab shows Order when below 16 and Fill up to 16
- **THEN** Save is greyed out and the tab says the minimum must be below Fill up to

#### Scenario: Nothing to remove
- **WHEN** the player opens the Rule tab for an item without a rule
- **THEN** Remove is greyed out and the steppers show 0 and one stack of the item
