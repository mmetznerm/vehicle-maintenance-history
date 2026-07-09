import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { TextField } from "./TextField";

describe("TextField", () => {
  it("renders label, input metadata and optional adornments", () => {
    render(
      <TextField
        id="plate"
        label="Plate"
        value="ABC-1234"
        placeholder="ABC-1234"
        autoComplete="off"
        leadingIcon={<span data-testid="leading-icon" />}
        trailingAction={<button type="button">Clear</button>}
        onChange={vi.fn()}
      />,
    );

    const input = screen.getByLabelText("Plate");

    expect(input).toHaveValue("ABC-1234");
    expect(input).toHaveAttribute("placeholder", "ABC-1234");
    expect(input).toHaveAttribute("autocomplete", "off");
    expect(screen.getByTestId("leading-icon")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Clear" })).toBeInTheDocument();
  });

  it("notifies callers when the value changes", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(<TextField id="name" label="Name" value="" onChange={onChange} />);

    await user.type(screen.getByLabelText("Name"), "AutoLog");

    expect(onChange).toHaveBeenCalledTimes(7);
    expect(onChange).toHaveBeenLastCalledWith("g");
  });
});
