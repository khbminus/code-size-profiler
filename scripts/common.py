from typing import List

BASE = 239
MOD = 1791791791
excluded = {}


def get_hashes(s: str) -> List[int]:
    n = len(s)
    hashes = [0] * (n + 1)
    for i, c in enumerate(s):
        hashes[i + 1] = (hashes[i] * BASE % MOD + ord(c)) % MOD
    return hashes


def check(x: str) -> bool:
    hsh = 0
    for i, c in enumerate(x):
        hsh = (hsh * BASE % MOD + ord(c)) % MOD
        if hsh in excluded.get(i + 1, set()):
            return True
    return False


def build(lst: List[str]):
    for s in lst:
        hashes = get_hashes(s)
        kst = excluded.get(len(s), set())
        kst.add(hashes[-1])
        excluded[len(s)] = kst
