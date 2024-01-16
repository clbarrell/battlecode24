# TODO List of things

In progress:

Not started

- Safe passage returning with flag. When I have a flag, put it in shared array and which spawn I'm going to. Then every other duck can make sure that they do not block the path of the flag carrier by whitelisting cells from FlagCarrier to Spawn within 4 cells of the FlagCarrier.
- How do I know where I got the flag from? so I can stop looking there.
- Have squads
- Bug nav: save the direction I was trying to go. Instead of getting Direction.to(dest), just keep trying idealDirection until it's visitable.
- Notify that someone is holding our flag, chase them
- round upgrade thing
- retreat if health low
- make judgement about hether to go to flag holder to attack or attack enemy I can attack
- if i  have flag, never move backwards
- account for up to 4 spawns
- fix invalid symmOptions late game - maybe I'mmaking 1 if there's no flag?
- make supporting dxdy to give extra space to the X and Y direction of our bases
- wait for a group before leaving base
- estimmate flag location from broadcast
- if I'm healing I can't attack. So only heal if I’m further away from the enemy, otherwise wait to attack
- Crowd source all the wall and dam cells in map, store them in arrays and then use that for BFS? Maybe it'll be faster by not having to do rc.senseLocation() every time
- attack; position so I'm only facing 1 enemy, but maybe more allies?
2. Move out of enemy range when not able to attack.
3. Retreat when low health to get healed by archon.
4. Multi-tiered pathfinding based on bytecode left.
5. Put enemy targets in shared array.
6. Prioritize low HP, dangerous enemies for targeting.
7. If surrounded by allied soldiers, advance even if already in range of target.
you move into range and then shoot, they take one turn to shoot, and then you shoot and move out of range; 
1v1, Nv1, NvN, considerAttackMove,
move or attack first?
to move into position to attack, use a pre generated array of positions, so that I can always go adjaceent to existing robots.
- In the first half of the game, move towards enemy more spaced out to minimise the impact of bombs! maybe record too if they are using bombs.
- if I'm moving to the enemy, and there's a friednly flag carrier, then leave space for them!

Bot name:

- Duck2

  - First basic bot working. Does all the things. No defense though basically, except for a few traps.
  - Instead of just guessing enemy spawns by my spawn locs, save the location of my flags (3) and use that.
  - use the priority flag thing.

- Duck3

  - Fill in water cells when blocked!!
  - Attack the enemy I can scan if they're holding our flag
  - Keep the flag carriers safe with a platoon
  - When in jail, find a good spawn location based on other data
  - Fix the losslessness of after I've picked up the flags

- Duck4
  - Defend your base with 3 defenders and make a surrounding pattern of traps
- - Defend. Make some traps near base. Have a squad build them.

- Duck 5
  - explore in first 150 turns
  - defend a different flag if your flag is gone
  - Improve pathfinding. Tried A* but too expensive. Made changes to bug nav to properly go around the obstacle. But it chooses the best direction to turn by looking at which path (left or right) gets it closer to the target location.
