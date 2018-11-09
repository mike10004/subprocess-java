#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys

"""Prints inputs in sequence on standard output"""


def broken_pipe():
    sys.stderr.close()
    exit(0)


def dump(ifile):
    n = 0
    while True:
        b = ifile.read(1)
        if len(b) == 0:
            break
        n += 1
        try:
            sys.stdout.buffer.write(b)
        except BrokenPipeError:
            broken_pipe()
    return n


def open_and_dump(pathname):
    if pathname == '-':
        return dump(sys.stdin.buffer)
    else:
        with open(pathname, 'rb', buffering=0) as ifile:
            return dump(ifile)


if __name__ == '__main__':
    args = sys.argv[1:]
    if not args:
        dump(sys.stdin.buffer)
    else:
        for arg in args:
            open_and_dump(arg)
    exit(0)
