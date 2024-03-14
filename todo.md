# TODO List of things

In progress:

Not started


- How do I know where I got the flag from? so I can stop looking there.
- Have squads
- Notify that someone is holding our flag, chase them

- make judgement about hether to go to flag holder to attack or attack enemy I can attack

- fix invalid symmOptions late game - maybe I'mmaking 1 if there's no flag?

- Flag
  - make supporting dxdy to give extra space to the X and Y direction of our bases
  - - if i  have flag, never move backwards
  - better organise supporting flank, only on enemy facing space.
  - if I see an ally carrying a flag, don't get in their way.
  - if I'm moving to the enemy, and there's a friednly flag carrier, then leave space for them!
  - write a function to generate a score for each location as a flag defending spot. during the explore phase, look at all cells around you and score them, save the spot if it has a higher score than is already in the list.
  - estimmate flag location from broadcast [nah]
  - defend flag; 5 nearest ducks retreat to it. save ur duck & location, 
  - flag bringer - report enemies and make a safe path back, use milestones
  - Factors; distance to enemy spawns (guessed - our side of the map) - or just distance to barrier, number of walls between cell and other sisde of map, choke points? a 'good' distance to other flags, not too close

- Spawn
  - wait for a group before leaving base
  - Only respawn in groups of 4. Leave some in reserve to spawn when we need to defend.
- if I'm healing I can't attack. So only heal if I’m further away from the enemy, otherwise wait to attack
- Crowd source all the wall and dam cells in map, store them in arrays and then use that for BFS? Maybe it'll be faster by not having to do rc.senseLocation() every time

- Attack micro
  -    you move into range and then shoot, they take one turn to shoot, and then you shoot and move out of range;
  - if there's too many enemies and I'm outnumbered run away
- if there's no enemies (we killed them), and there's lots of allies, and the average health of our allies is <80%, move tigher into the cluster and keep healing

5. Put enemy targets in shared array.


- In the first half of the game, move towards enemy more spaced out to minimise the impact of bombs! maybe record too if they are using bombs.

  
- get more specialisations. fewer ducks doing only 1 thing
- sneak attack flag, follow a wall/edge map around
- place traps only when none nextdoor. maybe get trap builder to do?
- better rotation guessing ? split map into quaerters. see if it matches the quarter i can see.
- defend checkered warter around base, slow down enemy
- if I'm holding flag and have less than 7 allies nearby, call for help!
  - Moving to enemy spawn when I'm holdin ghte last flag fucks up and robots don't go anywhere. oR it uses the wrong reflection

Bot name:

- Duck2

  - First basic bot working. Does all the things. No defense though basically, except for a few traps.
  - Instead of just guessing enemy spawns by my spawn locs, save the location of my flags (3) and use that.
  - use the priority flag thing.

- Duck3

  - Fill in water cells when blocked!!
  - Attack the enemy I can scan if they're holding our flag
  - Keep the flag carriers safe with a platoon - spread out
  - When in jail, find a good spawn location based on other data
  - Fix the losslessness of after I've picked up the flags

- Duck4
  - Defend your base with 3 defenders and make a surrounding pattern of traps
- - Defend. Make some traps near base. Have a squad build them.

- Duck 5

  - explore in first 150 turns
  - defend a different flag if your flag is gone
  - Improve pathfinding. Tried A\* but too expensive. Made changes to bug nav to properly go around the obstacle. But it chooses the best direction to turn by looking at which path (left or right) gets it closer to the target location.
  - round upgrade thing

- Duck 6

  - Proper attacking micro management. And like all the strategies. Moving backwards after attacking. Moving closer to allies, or doubling down and moving into a group. Retreating with low health.
  - attack; position so I'm only facing 1 enemy, but maybe more allies?
  - Move out of enemy range when not able to attack.

- Duck 7
  - Real actually working micro management attacking. Better tooling to inspect robot vision.
  - Make healing work properly! and not stuff up flag capture
  - Capturing flag bots ask for support, and people come. This works really well.
  