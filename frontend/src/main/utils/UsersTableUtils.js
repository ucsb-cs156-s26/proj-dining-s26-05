const toggleAdminMutation_params = (cell) => ({
  url: "/api/admin/toggleAdmin",
  method: "PUT",
  params: { id: cell.row.original.id },
});

const toggleModeratorMutation_params = (cell) => ({
  url: "/api/admin/toggleModerator",
  method: "PUT",
  params: { id: cell.row.original.id },
});

export { toggleAdminMutation_params, toggleModeratorMutation_params };
