// generate the map coords to paste into the java util file

let coords = [];

for (let x = -3; x < 4; x++) {
  for (let y = -3; y < 4; y++) {
    if ([-1, 0, 1].includes(x) && [-1, 0, 1].includes(y)) continue;
    coords.push([x, y]);
  }
}

// mix up the coords array
coords.sort(() => Math.random() - 0.5);

console.log(coords.map((c) => `{${c[0]}, ${c[1]}}`).join(", "));
/*
  - - - - - - - -  
  - - - - - - - -
  - - - X - - - -
  - - - - - - - -
  - - - - - - - -
*/

// directions
/*
  - - - - - - - -  
  - - 7 0 1 - - -
  - - 6 X 2 - - -
  - - 5 4 3 - - -
  - - - - - - - -
*/
// static final Direction[] directions = {
//   Direction.NORTH,
//   Direction.NORTHEAST,
//   Direction.EAST,
//   Direction.SOUTHEAST,
//   Direction.SOUTH,
//   Direction.SOUTHWEST,
//   Direction.WEST,
//   Direction.NORTHWEST,
// };

const ords = [0, 1, 2, 3, 4, 5, 6, 7];
const words = [
  "Direction.NORTH",
  "Direction.NORTHEAST",
  "Direction.EAST",
  "Direction.SOUTHEAST",
  "Direction.SOUTH",
  "Direction.SOUTHWEST",
  "Direction.WEST",
  "Direction.NORTHWEST",
];

// for each direction I'm facing, remove the opposite +/-

const directionsToCheck = [];

for (i = 0; i < ords.length; i++) {
  let o = ords[i];
  let opposite = (o + 4) % 8;
  let o1 = (opposite + 1) % 8;
  let o2 = (opposite - 1) % 8;
  let dirs = ords
    .map((x) => ([opposite, o1, o2].includes(x) ? null : x))
    .filter((x) => x != null);
  directionsToCheck.push(dirs);
}

console.log(
  "{",
  directionsToCheck.map((c) => c.map((n) => words[n]).join(",")).join("},{"),
  "}"
);

// x = 12
// x.toString(2)

// y = 31
// y.toString(2)

// xbin = x << 6
// xbin.toString(2)

// combined = (x << 6) | y
// combined.toString(2);

// g = ((1 << 1 | 1) << 1 | 1)
// g.toString(2)
