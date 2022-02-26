#!/usr/bin/env python3


import argparse


def parse_id_string(s):
    data = s.split('" = "')
    return data[0][1:], data[1][:-3]


def do_permission_file(file_in, file_out, file_permission):
    permission_ids = {}
    with open(file_permission, 'r') as f:
        for line in f:
            data = parse_id_string(line)
            permission_ids.update({data[0]: data[1]})

    permission_list = []
    with open(file_in, 'r') as f:
        for line in f:
            id_, translation = parse_id_string(line)
            if id_ in permission_ids.keys():
                s = '{} = "{}";\n'.format(permission_ids[id_], translation)
                permission_list.append(s)

    assert len(permission_list) == len(permission_ids)

    with open(file_out, 'w') as f:
        for line in permission_list:
            f.write(line)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', type=str)
    parser.add_argument('-o', '--output', type=str)
    parser.add_argument('-p', '--permission', help='File with ids of permission strings', type=str)

    args = parser.parse_args()

    do_permission_file(args.input, args.output, args.permission)


if __name__== '__main__':
    main()
