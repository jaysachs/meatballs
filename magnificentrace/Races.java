/*
 * Copyright 2020 Jay Sachs (jay@covariant.org)
 */

import java.util.*;

public abstract class Races<Player extends Enum<?>> {

  private final Random rand = new Random();
  private final Map<Player, Integer> loc = new HashMap<>();
  private final List<Player> players;
  private final Map<Player, Integer> rows = new HashMap<>();
  private final Options options;
  
  static class Options {
    Options(int runs, int bet, int spaces, int bonus) {
      this.bet = bet;
      this.spaces = spaces;
      this.runs = runs;
      this.bonus = bonus;
    }
    Options withBonus(int bonus) {
      return new Options(runs, bet, spaces, bonus);
    }
    private final int bet;
    private final int runs;
    private final int spaces;
    private final int bonus;
  }
  
  protected Races(Class<Player> clz, Options options) {
    try {
      players = Arrays.asList((Player[]) clz.getMethod("values").invoke(null));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.options = options;
  }
  
  protected int roll(int faces) {
    return rand.nextInt(faces) + 1;
  }

  protected void moveRow(Player p, int delta) {
    int result = rows.get(p) + delta;
    if (result < 0) {
      result = 0;
    }
    if (result >= players.size()) {
      result = players.size() - 1;
    }
    rows.put(p, result);
  }
  
  protected int move(Player p, int spaces) {
    int result = loc.get(p) + spaces;
    if (result < 0) {
      result = 0;
    }
    loc.put(p, result);
    return result;
  }

  abstract protected void handle(Player p);

  protected void postMoves() {
  }
  
  List<Player> oneRace() {
    for (Player p : players) {
      loc.put(p, 0);
      rows.put(p, p.ordinal());
    }

    while (true) {
      for (Player p : players) {
	handle(p);
      }
      postMoves();
      List<Player> winner = checkWin();
      if (winner != null) {
	return winner;
      }
    }
  }

  protected Player last() {
    int m = options.spaces * 20;
    Player result = null;
    for (Player p : players) {
      if (loc.get(p) <= m) {
	m = loc.get(p);
	result = p;
      }
    }
    if (result == null) {
      System.err.println("Null last: " + loc);
    }
    return result;
  }

  protected int location(Player p) {
    return loc.get(p);
  }

  protected int row(Player p) {
    return rows.get(p);
  }

  private List<Player> checkWin() {
    if (loc.values().stream().allMatch(x -> x < options.spaces)) {
      return null;
    }
    List places = new ArrayList<>(players);
    Collections.sort(places,
		     (Player x, Player y) -> {
		       int p1 = loc.get(x);
		       int p2 = loc.get(y);
		       if (p1 == p2) {
			 return rows.get(y) - rows.get(x);
		       }
		       return p2 - p1;
		     });
    return places;
  }

  void run() {
    Map<Player, Integer> wins = new LinkedHashMap<>();
    for (Player p : players) {
      wins.put(p, 0);
    }
    Map<Player, Double> returns = new LinkedHashMap<>();
    for (Player p : players) {
      returns.put(p, 0.0);
    }
    for (int i = 0; i < options.runs; i++) {
      List<Player> winner = oneRace();
      wins.put(winner.get(0), wins.get(winner.get(0)) + 1);
      
      returns.put(winner.get(0), returns.get(winner.get(0)) + 2 * options.bet);
      returns.put(winner.get(2), returns.get(winner.get(2)) - options.bet);
      returns.put(winner.get(3), returns.get(winner.get(3)) - options.bet);
      for (Player p : players) {
	if (winner.indexOf(p) < winner.indexOf(players.get(3))) {
	  returns.put(p, returns.get(p) + 5 * options.bonus);
	}
      }
    }
    for (Player p : players) {
      returns.put(p, returns.get(p) / options.runs);
    }
    String name = getClass().getName();
    System.out.println(name.substring(name.indexOf('$')+1) + ":");
    System.out.println(wins);
    System.out.println(returns);
  }
  
  static class HareAndTortoise extends Races<HareAndTortoise.Player> {
    enum Player {
      Hare,
      Tortoise,
      Porcupine,
      Dan
    };

    HareAndTortoise(Options options) {
      super(Player.class, options);
    }

    @Override
    protected void handle(Player p) {
      switch (p) {
	case Hare:
	  if (roll(2) == 1) {
	    move(p, 10);
	  }
	  break;
	case Tortoise:
	  move(p, roll(4) * 2);
	  break;
	case Porcupine:
	  {
	    int i = roll(8);
	    move(p, i);
	    if (i == 8) {
	      move(Player.Hare, -3);
	      move(Player.Tortoise, -2);
	      move(Player.Dan, -2);
	    }
	  }
	  break;
	case Dan:
	  move(p, 4);
	}
    }
  }

  static class MagicalAthletes extends Races<MagicalAthletes.Player> {
    enum Player {
      Conjurer,
      Priest,
      Martial,
      Dan
    };

    MagicalAthletes(Options options) {
      super(Player.class, options);
    }
    
    @Override
    protected void handle(Player p) {
      switch (p) {
      case Conjurer:
	{
	  int i = roll(6);
	  if (i <= 3) {
	    i = roll(6);
	  }
	  while (i == 1) {
	    i = roll(6);
	  }
	  move(p, i);
	}
	  break;
      case Priest:
	{
	  move(last(), 2);
	  move(p, roll(6) + 1);
	}
	break;
      case Martial:
	{
	  int i = roll(6);
	  while (i > 0) {
	    int next = move(p, 1);
	    if (location(Player.Conjurer) != next
		&& location(Player.Priest) != next
		&& location(Player.Dan) != next) {
	      i--;
	    }
	  }
	}
	break;
      case Dan:
	move(p, 4);
      }
    }
  }

  static class FormulaD extends Races<FormulaD.Player> {
    enum Player {
      Yellow,
      Red,
      Black,
      Dan
    }

    private int yellowspeed = 0;
    private int redgear = 4;

    FormulaD(Options options) {
      super(Player.class, options);
    }

    @Override
    protected void handle(Player p) {
      switch (p) {
      case Yellow:
	{
	  switch (roll(4)) {
	  case 1: yellowspeed = -1; break;
	  case 2: yellowspeed = 3; break;
	  case 3: yellowspeed = 6; break;
	  case 4: yellowspeed = 10;
	  }
	  move(p, yellowspeed);
	}
	break;
      case Red:
	switch (roll(8)) {
	case 1: redgear = Math.min(1, redgear - 1); break;
	case 7: redgear = Math.max(10, redgear + 1); break;
	case 8: redgear = Math.max(10, redgear + 2); break;
	}
	move(p, redgear);
	break;
      case Black:
	switch (roll(6)) {
	case 4: move(p, yellowspeed * 2); break;
	case 5: move(p, redgear * 2); break;
	case 6: move(p, 8); break;
	}
	break;
      case Dan:
	move(p, 4);
      }
    }
  }

  static class RoboRally extends Races<RoboRally.Player> {
    enum Player {
      Hammerbot,
      Twonky,
      Squashbot,
      Dan
    }

    RoboRally(Options options) {
      super(Player.class, options);
    }

    @Override
    protected void handle(Player p) {
      switch (p) {
      case Hammerbot:
	switch (roll(4)) {
	case 1: move(p, 1); break;
	case 4: move(p, 13); break;
	}
	break;
      case Twonky: {
	int i = roll(6);
	move(p, i);
	moveRow(p, (i % 2 == 0) ? -1 : 1);
	// fire lasers
	for (Player x : Player.values()) {
	  if (x != p && row(x) == row(p)) {
	    int y = move(x, -2);
	  }
	}
      }
	break;
      case Squashbot: {
	int i = roll(8);
	if (i < 8) {
	  move(p, i);
	} else {
	  move(p, 3);
	  move(Player.Dan, -8);
	}
      }
	break;
      case Dan:
	move(p, 4);
      }
    }
    @Override protected void postMoves() {
      for (Player x : Player.values()) {
	if (x != Player.Twonky && row(x) == row(Player.Twonky)) {
	  if (location(x) == location(Player.Twonky)) {
	    move(x, -1);
	  }
	}
      }
    }
  }

  static Options parseOptions(String[] args) {
    List<String> a = new ArrayList<>(Arrays.asList(args));
    int runs = 10000;
    int spaces = 50;
    int bonus = 5;
    int bet = 1;
    if (!a.isEmpty()) {
      runs = Integer.valueOf(a.remove(0));
    }
    if (!a.isEmpty()) {
      spaces = Integer.valueOf(a.remove(0));
    }
    if (!a.isEmpty()) {
      bonus = Integer.valueOf(a.remove(0));
    }
    if (!a.isEmpty()) {
      bet = Integer.valueOf(a.remove(0));
    }
    if (!a.isEmpty()) {
      throw new RuntimeException("too many arguments");
    }
    return new Options(runs, bet, spaces, bet);
  }

  public static void main(String[] args) {
    Options options = parseOptions(args);
    new HareAndTortoise(options).run();
    System.out.println();
    new MagicalAthletes(options.withBonus(options.bonus * 2)).run();
    System.out.println();
    new FormulaD(options.withBonus(options.bonus * 3)).run();
    System.out.println();
    new RoboRally(options.withBonus(options.bonus * 4)).run();
  }
}
