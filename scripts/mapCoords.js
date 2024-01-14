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

