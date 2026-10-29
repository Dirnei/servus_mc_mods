"""Generates the shuttle drive sound loops.

This script is the source of the files; change the sound here, not the OGGs.

    python tools/shuttle-sounds/gen.py                 writes the six loops
    python tools/shuttle-sounds/gen.py --wav DIR       also writes each loop as WAV into DIR

Needs numpy and ffmpeg (with libvorbis) on the PATH. Written into
shuttles/src/main/resources/assets/servus_shuttles/sounds/:
shuttle_drive_1..3.ogg, three takes of the drive loop from three seeds, and
shuttle_drive_muffled_1..3.ogg, the same takes low passed as if heard through a wall.

A take is 10 s of mono: a two layer rumble over a faint hiss, each layer swelling slowly and at random,
sparse dull wheel clicks at an irregular pace with some skipped, and a short loose
rattle now and then. Every filter works on the whole take as one period (FFT), and
clicks and rattles that run past the end wrap round to the start, so the take loops
without a seam.
"""
import os
import subprocess
import sys
import tempfile

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'shuttles', 'src', 'main', 'resources', 'assets', 'servus_shuttles', 'sounds')
RATE = 44100
SECONDS = 10
LENGTH = RATE * SECONDS
SEEDS = (1101, 2202, 3303)

TARGET_RMS = 0.053
MUFFLE_CUTOFF_HZ = 600.0

LOW_RUMBLE = (35.0, 110.0, 1.0)
MID_RUMBLE = (110.0, 380.0, 0.55)
HISS = (380.0, 0.3, 2.0)
SWELL_HZ = 0.6
SWELL_DEPTH = 0.45

CLICK_GAP_S = 0.6
CLICK_GAP_JITTER = 0.35
CLICK_SKIP = 0.35
CLICK_PEAK = 2.6
CLICK_CUTOFF_HZ = 1400.0

RATTLES = (1, 3)
RATTLE_TICKS = (6, 11)
RATTLE_SPAN_S = (0.12, 0.26)
RATTLE_BAND = (350.0, 2400.0)
RATTLE_PEAK = 2.0


def frequencies():
    return np.fft.rfftfreq(LENGTH, 1.0 / RATE)


def band(signal, low, high, slope=0.15):
    """Band passes one period of the signal with soft (raised cosine) edges, each about slope times its corner wide."""
    f = frequencies()
    gain = np.ones_like(f)
    if low > 0:
        gain *= edge(f, low, slope, rising=True)
    if high is not None:
        gain *= edge(f, high, slope, rising=False)
    return np.fft.irfft(np.fft.rfft(signal) * gain, LENGTH)


def edge(f, corner, slope, rising):
    width = corner * slope
    t = np.clip((f - (corner - width)) / (2 * width), 0.0, 1.0)
    up = 0.5 - 0.5 * np.cos(np.pi * t)
    return up if rising else 1.0 - up


def normalised(signal):
    return signal / np.sqrt(np.mean(signal ** 2))


def peak_normalised(signal):
    return signal / np.max(np.abs(signal))


def swell(rng):
    """A slow random level between 1 - depth and 1 + depth that wraps round seamlessly."""
    slow = normalised(band(rng.standard_normal(LENGTH), 0.0, SWELL_HZ, slope=0.5))
    return 1.0 + SWELL_DEPTH * np.tanh(slow / 1.5)


def rumble(rng):
    total = np.zeros(LENGTH)
    for low, high, level in (LOW_RUMBLE, MID_RUMBLE):
        layer = normalised(band(rng.standard_normal(LENGTH), low, high))
        total += level * layer * swell(rng)
    corner, level, fall = HISS
    return total + level * normalised(falling(rng.standard_normal(LENGTH), corner, fall)) * swell(rng)


def falling(signal, corner, fall):
    """Keeps the part of the signal above the corner, falling off by fall times 6 dB per octave above it."""
    f = frequencies()
    gain = edge(f, corner, 0.15, rising=True) * (corner / np.maximum(f, corner)) ** fall
    return np.fft.irfft(np.fft.rfft(signal) * gain, LENGTH)


def add_wrapped(signal, start, burst):
    idx = (start + np.arange(len(burst))) % LENGTH
    np.add.at(signal, idx, burst)


def thump(rng, length_s, decay_s):
    n = int(length_s * RATE)
    t = np.arange(n) / RATE
    return rng.standard_normal(n) * np.exp(-t / decay_s)


def clicks(rng):
    """Wheel clicks on rail joints: a ta-tak pair at an irregular pace, some skipped."""
    signal = np.zeros(LENGTH)
    at = rng.uniform(0, CLICK_GAP_S) * RATE
    while at < LENGTH:
        if rng.random() >= CLICK_SKIP:
            level = rng.uniform(0.5, 1.0)
            add_wrapped(signal, int(at), level * thump(rng, 0.05, 0.008))
            add_wrapped(signal, int(at + rng.uniform(0.07, 0.11) * RATE), 0.7 * level * thump(rng, 0.05, 0.007))
        at += CLICK_GAP_S * (1 + rng.uniform(-CLICK_GAP_JITTER, CLICK_GAP_JITTER)) * RATE
    return peak_normalised(band(signal, 60.0, CLICK_CUTOFF_HZ, slope=0.4))


def rattles(rng):
    """A loose part shaking for a moment: a quick run of small ticks of falling strength."""
    signal = np.zeros(LENGTH)
    for _ in range(rng.integers(RATTLES[0], RATTLES[1] + 1)):
        start = rng.uniform(0, SECONDS) * RATE
        span = rng.uniform(*RATTLE_SPAN_S) * RATE
        ticks = rng.integers(RATTLE_TICKS[0], RATTLE_TICKS[1] + 1)
        for i, offset in enumerate(np.sort(rng.uniform(0, span, ticks))):
            fade = 1.0 - 0.7 * i / ticks
            add_wrapped(signal, int(start + offset), fade * rng.uniform(0.4, 1.0) * thump(rng, 0.02, 0.003))
    return peak_normalised(band(signal, RATTLE_BAND[0], RATTLE_BAND[1], slope=0.3))


def take(seed):
    rng = np.random.default_rng(seed)
    mix = normalised(rumble(rng)) + CLICK_PEAK * clicks(rng) + RATTLE_PEAK * rattles(rng)
    return mix * (TARGET_RMS / np.sqrt(np.mean(mix ** 2)))


def muffled(signal):
    return band(signal, 0.0, MUFFLE_CUTOFF_HZ, slope=0.35)


def write_ogg(signal, path):
    pcm = np.clip(signal, -1.0, 1.0).astype('<f4')
    with tempfile.TemporaryDirectory() as tmp:
        raw = os.path.join(tmp, 'take.f32')
        pcm.tofile(raw)
        subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-f', 'f32le', '-ar', str(RATE), '-ac', '1',
                        '-i', raw, '-c:a', 'libvorbis', '-q:a', '6', path], check=True)


def write_wav(signal, path):
    pcm = (np.clip(signal, -1.0, 1.0) * 32767).astype('<i2')
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', str(RATE), '-ac', '1',
                    '-i', '-', path], input=pcm.tobytes(), check=True)


def main():
    wav_dir = sys.argv[2] if len(sys.argv) == 3 and sys.argv[1] == '--wav' else None
    os.makedirs(OUT, exist_ok=True)
    for number, seed in enumerate(SEEDS, start=1):
        normal = take(seed)
        for name, signal in ((f'shuttle_drive_{number}', normal), (f'shuttle_drive_muffled_{number}', muffled(normal))):
            write_ogg(signal, os.path.join(OUT, name + '.ogg'))
            if wav_dir:
                os.makedirs(wav_dir, exist_ok=True)
                write_wav(signal, os.path.join(wav_dir, name + '.wav'))


if __name__ == '__main__':
    main()
