import OurTable, { ButtonColumn } from "main/components/OurTable";
import { useBackendMutation } from "main/utils/useBackend";
import {
  toggleAdminMutation_params,
  toggleModeratorMutation_params,
} from "main/utils/UsersTableUtils";

const columns = [
  {
    Header: "id",
    accessor: "id",
  },
  {
    Header: "First Name",
    accessor: "givenName",
  },
  {
    Header: "Last Name",
    accessor: "familyName",
  },
  {
    Header: "Email",
    accessor: "email",
  },
  {
    Header: "Admin",
    id: "admin",
    accessor: (row, _rowIndex) => String(row.admin),
  },
  {
    Header: "Moderator",
    id: "moderator",
    accessor: (row, _rowIndex) => String(row.moderator),
  },
  {
    Header: "Alias",
    accessor: "alias",
  },
  {
    Header: "Proposed Alias",
    accessor: "proposedAlias",
  },
  {
    Header: "Status",
    accessor: (row) => {
      if (row.status === "Approved" && row.dateApproved) {
        const [year, month, day] = row.dateApproved.split("-");
        const formattedDate = new Date(
          year,
          month - 1,
          day,
        ).toLocaleDateString();
        return `Approved on ${formattedDate}`;
      }
      return row.status;
    },
  },
];

export default function UsersTable({ users }) {
  // Stryker disable all
  const toggleAdminMutation = useBackendMutation(
    toggleAdminMutation_params,
    {},
    ["/api/admin/users"],
  );

  const toggleModeratorMutation = useBackendMutation(
    toggleModeratorMutation_params,
    {},
    ["/api/admin/users"],
  );
  // Stryker restore all

  // Stryker disable next-line all
  const toggleAdminCallback = async (cell) => {
    toggleAdminMutation.mutate(cell);
  };

  // Stryker disable next-line all
  const toggleModeratorCallback = async (cell) => {
    toggleModeratorMutation.mutate(cell);
  };

  const allColumns = [
    ...columns,
    ButtonColumn("Toggle Admin", "primary", toggleAdminCallback, "UsersTable"),
    ButtonColumn(
      "Toggle Moderator",
      "primary",
      toggleModeratorCallback,
      "UsersTable",
    ),
  ];

  return <OurTable data={users} columns={allColumns} testid={"UsersTable"} />;
}
