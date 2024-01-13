# TODO List of things

In progress:


Not started
- Safe passage returning with flag. When I have a flag, put it in shared array and which spawn I'm going to. Then every other duck can make sure that they do not block the path of the flag carrier by whitelisting cells from FlagCarrier to Spawn within 4 cells of the FlagCarrier.
- How do I know where I got the flag from? so I can stop looking there.
- Have squads
- Defend. Make some traps near base. Have a squad build them.
- Bug nav: save the direction I was trying to go. Instead of getting Direction.to(dest), just keep trying idealDirection until it's visitable.
- Notify that someone is holding our flag, chase them
- round upgrade thing
- retreat if health low
- make judgement about hether to go to flag holder to attack or attack enemy I can attack
- if i  have flag, never move backwards
- account for up to 4 spawns
- fix invalid symmOptions late game -  maybe I'mmaking 1 if there's no flag?
- make supporting dxdy to give extra space to the X and Y direction of our bases
- wait for a group before leaving base
- estimmate flag location from broadcast

Bot name:
- Duck2
  - First basic bot working. Does all the things. No defense though basically, except for a few traps.
  - Instead of just guessing enemy spawns by my spawn locs, save the location of my flags (3) and use that.
  - use the priority flag thing.

- Duck3
  - Fill in water cells when blocked!!
  - Attack the enemy I can scan if they're holding our flag
  - Keep the flag carriers safe.
  - When in jail, find a good spawn location based on other data
  - Fix the losslessness of after I've picked up the flags

