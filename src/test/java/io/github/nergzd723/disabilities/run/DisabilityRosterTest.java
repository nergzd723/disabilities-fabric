package io.github.nergzd723.disabilities.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.nergzd723.disabilities.Disability;

class DisabilityRosterTest {
	private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID STEVE = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void replaceIsTheDefaultMode() {
		assertEquals(TimerMode.REPLACE, new DisabilityRoster().mode());
	}

	@Test
	void replaceCuresEverythingAndRollsExactlyOneNewDisability() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.give(ALEX, Disability.BLIND);
		roster.give(ALEX, Disability.DEAF);
		roster.give(ALEX, Disability.MUTE);
		Random random = new Random(42);

		for (int i = 0; i < 200; i++) {
			Set<Disability> before = EnumSet.copyOf(roster.get(ALEX));
			DisabilityRoster.Progression result = roster.progress(ALEX, random);

			assertEquals(before, result.cured());
			assertTrue(result.gained().isPresent());
			assertEquals(Set.of(result.gained().get()), roster.get(ALEX), "only the new disability is left");
			assertFalse(before.contains(result.gained().get()), "never re-rolls something that was just cured");
		}
	}

	@Test
	void replaceNeverStacksOverManyTimerCycles() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.setIntervalTicks(20);
		Random random = new Random(7);
		for (int tick = 0; tick < 20 * 500; tick++) {
			if (roster.tickTimer()) {
				roster.progress(ALEX, random);
				assertEquals(1, roster.get(ALEX).size());
			}
		}
	}

	@Test
	void replaceWithEveryDisabilityStillRollsOne() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.giveAll(ALEX);

		DisabilityRoster.Progression result = roster.progress(ALEX, new Random(1));

		assertEquals(Disability.values().length, result.cured().size());
		assertEquals(1, roster.get(ALEX).size());
	}

	@Test
	void replaceClearsCancerProgress() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.give(ALEX, Disability.CANCER);
		roster.setCancerHealthLost(ALEX, 6);

		roster.progress(ALEX, new Random(3));

		assertEquals(0, roster.cancerHealthLost(ALEX));
	}

	@Test
	void stackModeAddsOnTopUntilEverythingIsAcquired() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.setMode(TimerMode.STACK);
		Random random = new Random(11);

		for (int i = 1; i <= Disability.values().length; i++) {
			DisabilityRoster.Progression result = roster.progress(ALEX, random);
			assertTrue(result.cured().isEmpty());
			assertTrue(result.gained().isPresent());
			assertEquals(i, roster.get(ALEX).size());
		}
		assertTrue(roster.progress(ALEX, random).gained().isEmpty(), "nothing left to give");
	}

	@Test
	void playersAreIndependent() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.give(ALEX, Disability.SHORT);
		roster.progress(STEVE, new Random(5));

		assertEquals(Set.of(Disability.SHORT), roster.get(ALEX));
		assertEquals(1, roster.get(STEVE).size());
	}

	@Test
	void timerFiresOncePerIntervalAndRestarts() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.setIntervalTicks(100);
		int fired = 0;
		for (int tick = 0; tick < 1000; tick++) {
			if (roster.tickTimer()) {
				fired++;
				assertEquals(100, roster.ticksUntilNext());
			}
		}
		assertEquals(10, fired);
	}

	@Test
	void disabledTimerNeverFires() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.setIntervalTicks(20);
		roster.setTimerEnabled(false);
		for (int tick = 0; tick < 100; tick++) {
			assertFalse(roster.tickTimer());
		}
	}

	@Test
	void settingTheIntervalRestartsAndEnablesTheTimer() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.tickTimer();
		roster.setTimerEnabled(false);
		roster.setIntervalTicks(5);

		assertTrue(roster.timerEnabled());
		assertEquals(DisabilityRoster.MIN_INTERVAL_TICKS, roster.intervalTicks(), "clamped to one second");
		assertEquals(roster.intervalTicks(), roster.ticksUntilNext());
	}

	@Test
	void resetCuresEveryoneButKeepsTimerSettings() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.setIntervalTicks(400);
		roster.setMode(TimerMode.STACK);
		roster.give(ALEX, Disability.ADHD);
		roster.give(STEVE, Disability.OCD);
		roster.tickTimer();

		roster.resetAll();

		assertTrue(roster.get(ALEX).isEmpty());
		assertTrue(roster.get(STEVE).isEmpty());
		assertEquals(400, roster.ticksUntilNext());
		assertEquals(TimerMode.STACK, roster.mode());
	}

	@Test
	void giveAndRemoveReportWhetherAnythingChanged() {
		DisabilityRoster roster = new DisabilityRoster();
		assertTrue(roster.give(ALEX, Disability.VERTIGO));
		assertFalse(roster.give(ALEX, Disability.VERTIGO));
		assertTrue(roster.remove(ALEX, Disability.VERTIGO));
		assertFalse(roster.remove(ALEX, Disability.VERTIGO));
		assertTrue(roster.removeRandom(ALEX, new Random()).isEmpty());
	}

	@Test
	void changeListenerFiresForChangesButNotForCountdownTicks() {
		DisabilityRoster roster = new DisabilityRoster();
		AtomicInteger changes = new AtomicInteger();
		roster.setChangeListener(changes::incrementAndGet);

		roster.tickTimer();
		assertEquals(0, changes.get());

		roster.give(ALEX, Disability.DYSLEXIA);
		roster.setMode(TimerMode.STACK);
		roster.setTimerEnabled(false);
		assertEquals(3, changes.get());
	}

	@Test
	void restoreRoundTripsPlayers() {
		DisabilityRoster roster = new DisabilityRoster();
		roster.give(ALEX, Disability.CANCER);
		roster.give(ALEX, Disability.ASTHMA);
		roster.setCancerHealthLost(ALEX, 4);
		roster.setIntervalTicks(600);
		roster.tickTimer();

		DisabilityRoster copy = DisabilityRoster.restore(roster.intervalTicks(), roster.elapsedTicks(), roster.timerEnabled(), roster.mode(),
				roster.players());

		assertEquals(roster.get(ALEX), copy.get(ALEX));
		assertEquals(4, copy.cancerHealthLost(ALEX));
		assertEquals(roster.ticksUntilNext(), copy.ticksUntilNext());
		assertEquals(List.of(), DisabilityRoster.restore(600, 0, true, TimerMode.REPLACE, List.of()).players());
	}

	@Test
	void masksRoundTrip() {
		EnumSet<Disability> set = EnumSet.of(Disability.BLIND, Disability.ANOSOGNOSIA, Disability.OCD);
		assertEquals(set, Disability.fromMask(Disability.toMask(set)));
		assertEquals(Disability.KLEPTOMANIA, Disability.byId("Kleptomania").orElseThrow());
	}
}
