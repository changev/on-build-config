#!/usr/bin/env python

try:
    import xml.etree.cElementTree as ET
except ImportError:
    import xml.etree.ElementTree as ET
import sys
import os
import argparse

def parse_args(args):
    """
    Parse script arguments.
    :return: Parsed args for assignment
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-result-file',
                        required=True,
                        help="The file path of test result",
                        action='store')

    parser.add_argument('--parameters-file',
                        help="The jenkins parameter file that will used for succeeding Jenkins job",
                        action='store',
                        default="downstream_parameters")

    parsed_args = parser.parse_args(args)
    return parsed_args

def get_summary(root):
    summary = {}
    summary["tests"] = root.get("tests")
    summary["errors"] =  root.get("errors")
    summary["failures"] = root.get("failures")
    summary["skipped"] = root.get("skipped")
    summary["time"] = root.get("time")
    return summary

def write_parameters(filename, params):
    """
    Add/append parameters(java variable value pair) to the given parameter file.
    If the file does not exist, then create the file.
    :param filename: The path of the parameter file
    :param params: the parameters dictionary
    :return:None on success
            Raise any error if there is any
    """
    if filename is None:
        raise ValueError("parameter file name is not None")
    with open(filename, 'w') as fp:
        for key in params:
            entry = "{key}={value}\n".format(key=key, value=params[key])
            fp.write(entry)


def main():
    args = parse_args(sys.argv[1:])
    root = ET.parse(args.test_result_file).getroot()
    result_summary = get_summary(root)
    write_parameters(args.parameters_file, result_summary)

if __name__ == '__main__':
    main()
